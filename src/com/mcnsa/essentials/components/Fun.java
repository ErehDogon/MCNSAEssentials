package com.mcnsa.essentials.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.mcnsa.essentials.annotations.Command;
import com.mcnsa.essentials.utilities.ColourHandler;
import com.mcnsa.essentials.utilities.PlayerSelector;

public class Fun {
	private static boolean broadcast = true;
	private static int broadcastLimit = 6;
	
	@Command(command = "slap",
			arguments = {"target player[s]"},
			description = "slaps target player[s]",
			permissions = {"fun.slap"})
	public static boolean slap(CommandSender sender, String targetPlayer) {
		// get a list of all target players
		ArrayList<Player> targetPlayers = PlayerSelector.selectPlayersExact(targetPlayer);
		
		// make sure we have at least one target player
		if(targetPlayers.size() == 0) {
			ColourHandler.sendMessage(sender, "&cI couldn't find / parse target player[s] '" + targetPlayer + "' to slap!");
			return false;
		}
		
		// get our random number generator
		Random random = new Random();
		
		// loop through all target players
		int count = 0;
		for(Iterator<Player> it = targetPlayers.iterator(); it.hasNext();) {
			// get the player
			Player target = it.next();
			
			// set their velocity
			target.setVelocity(new Vector(
					random.nextDouble() * 2.0 - 1,
					random.nextDouble(),
					random.nextDouble() * 2.0 - 1));
			
			// and alert them!
			if(sender.getName().equals(target.getName())) {
				ColourHandler.sendMessage(target, "&eYou slapped yourself!");
			}
			else {
				ColourHandler.sendMessage(target, "&eYou were slapped by " + sender.getName() + "!");
			}
			
			// broadcast?
			if(broadcast && count < broadcastLimit) {
				Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers();
				for(int i = 0; i < onlinePlayers.length; i++) {
					if(!target.equals(onlinePlayers[i]) && !sender.equals(onlinePlayers[i])) {
						ColourHandler.sendMessage(onlinePlayers[i], "&e" + target.getName() + " was slapped by " + sender.getName() + "!");
					}
				}
			}
			
			count++;
		}
		if(count >= broadcastLimit) {
			Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers();
			for(int i = 0; i < onlinePlayers.length; i++) {
				ColourHandler.sendMessage(onlinePlayers[i], "&eand many more people...");
			}
		}
		
		return true;
	}
	
	@Command(command = "rocket",
			arguments = {"target player[s]"},
			description = "rockets target player[s]",
			permissions = {"fun.rocket"})
	public static boolean rocket(CommandSender sender, String targetPlayer) {
		// get a list of all target players
		ArrayList<Player> targetPlayers = PlayerSelector.selectPlayersExact(targetPlayer);
		
		// make sure we have at least one target player
		if(targetPlayers.size() == 0) {
			ColourHandler.sendMessage(sender, "&cI couldn't find / parse target player[s] '" + targetPlayer + "' to rocket!");
			return false;
		}
		
		// loop through all target players
		int count = 0;
		for(Iterator<Player> it = targetPlayers.iterator(); it.hasNext();) {
			// get the player
			Player target = it.next();
			
			// set their velocity
			target.setVelocity(new Vector(0, 4, 0));
			
			// and alert them!
			if(sender.getName().equals(target.getName())) {
				ColourHandler.sendMessage(target, "&eYou rocketed yourself!");
			}
			else {
				ColourHandler.sendMessage(target, "&eYou were rocketed by " + sender.getName() + "!");
			}
			
			// broadcast?
			if(broadcast && count < broadcastLimit) {
				Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers();
				for(int i = 0; i < onlinePlayers.length; i++) {
					if(!target.equals(onlinePlayers[i]) && !sender.equals(onlinePlayers[i])) {
						ColourHandler.sendMessage(onlinePlayers[i], "&e" + target.getName() + " was rocketed by " + sender.getName() + "!");
					}
				}
			}
			
			count++;
		}
		if(count >= broadcastLimit) {
			Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers();
			for(int i = 0; i < onlinePlayers.length; i++) {
				ColourHandler.sendMessage(onlinePlayers[i], "&eand many more people...");
			}
		}
		
		return true;
	}
	
	@Command(command = "immolate",
			arguments = {"target player[s]"},
			description = "immolates target player[s]",
			permissions = {"fun.immolate"})
	public static boolean immolate(CommandSender sender, String targetPlayer) {
		// get a list of all target players
		ArrayList<Player> targetPlayers = PlayerSelector.selectPlayersExact(targetPlayer);
		
		// make sure we have at least one target player
		if(targetPlayers.size() == 0) {
			ColourHandler.sendMessage(sender, "&cI couldn't find / parse target player[s] '" + targetPlayer + "' to immolate!");
			return false;
		}
		
		// loop through all target players
		int count = 0;
		for(Iterator<Player> it = targetPlayers.iterator(); it.hasNext();) {
			// get the player
			Player target = it.next();
			
			// light them on fire
			target.setFireTicks(240);
			
			// and alert them!
			if(sender.getName().equals(target.getName())) {
				ColourHandler.sendMessage(target, "&eYou immolated yourself!");
			}
			else {
				ColourHandler.sendMessage(target, "&eYou were immolated by " + sender.getName() + "!");
			}
			
			// broadcast?
			if(broadcast && count < broadcastLimit) {
				Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers();
				for(int i = 0; i < onlinePlayers.length; i++) {
					if(!target.equals(onlinePlayers[i]) && !sender.equals(onlinePlayers[i])) {
						ColourHandler.sendMessage(onlinePlayers[i], "&e" + target.getName() + " was immolated by " + sender.getName() + "!");
					}
				}
			}
			
			count++;
		}
		if(count >= broadcastLimit) {
			Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers();
			for(int i = 0; i < onlinePlayers.length; i++) {
				ColourHandler.sendMessage(onlinePlayers[i], "&eand many more people...");
			}
		}
		
		return true;
	}
}
