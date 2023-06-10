import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;

public class MoguraPVP extends JavaPlugin implements Listener {
    private boolean battleInProgress;
    private int countdown;
    private Scoreboard scoreboard;
    private Objective objective;

    @Override
    public void onEnable() {
        battleInProgress = false;
        countdown = 20 * 60; // 20分

        getServer().getPluginManager().registerEvents(this, this);
        createScoreboard();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (battleInProgress) {
            Player player = event.getPlayer();
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (battleInProgress) {
            checkEndCondition();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (battleInProgress) {
            Player player = event.getEntity();
            player.setGameMode(GameMode.SPECTATOR);
            checkEndCondition();
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!battleInProgress) {
            String command = event.getMessage().toLowerCase();
            if (command.equals("/start")) {
                startBattle();
                event.setCancelled(true);
            }
        }
    }

    private void startBattle() {
        battleInProgress = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("バトルが始まりました！");
        }

        new BukkitRunnable() {
            int timer = 5 * 60; // 5分

            @Override
            public void run() {
                if (timer <= 0) {
                    cancel();
                    startCountdown();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage("バトルに備えてください！ 残り" + timer + "秒");
                }

                timer--;
            }
        }.runTaskTimer(this, 0, 20); // 1秒間隔
    }

    private void startCountdown() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown <= 0) {
                    cancel();
                    endBattle();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    displayCoordinates(player);
                    updateScoreboard();
                }

                countdown--;
            }
        }.runTaskTimer(this, 0, 20); // 1秒間隔
    }

    private void endBattle() {
        battleInProgress = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("バトルが終了しました！");
            player.setGameMode(GameMode.SPECTATOR);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        scoreboard.clearSlot(DisplaySlot.SIDEBAR);
    }

    private void displayCoordinates(Player player) {
        Location location = player.getLocation();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        player.sendTitle("", "座標: X=" + x + ", Y=" + y + ", Z=" + z, 0, 20, 10);
    }


    private void createScoreboard() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        scoreboard = scoreboardManager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("battle", "dummy", "バトル");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        updateScoreboard();
    }

    private void updateScoreboard() {
        objective.getScore("時間: " + formatTime(countdown)).setScore(0);
        objective.getScore("プレイヤー数: " + getAlivePlayerCount()).setScore(-1);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    private String formatTime(int timeInSeconds) {
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private int getAlivePlayerCount() {
        int count = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                count++;
            }
        }

        return count;
    }

    private void checkEndCondition() {
        if (getAlivePlayerCount() <= 1) {
            countdown = 0;
        }
    }
}
