package com.mcnsa.essentials.managers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mcnsa.essentials.annotations.Command;
import com.mcnsa.essentials.exceptions.EssentialsCommandException;
import com.mcnsa.essentials.managers.ComponentManager.Component;
import com.mcnsa.essentials.utilities.ColourHandler;
import com.mcnsa.essentials.utilities.Logger;

public class CommandsManager implements CommandExecutor {
	// keep track of all known aliases we're using
	private HashSet<String> knownAliases = new HashSet<String>();
	
	// an ``internal'' command structure class to inject into the commandmap with
	public class EssentialsCommand extends org.bukkit.command.Command {
		// keep track of our command executor
		private CommandExecutor commandExecutor = null;
		
		// just call our default constructor
		protected EssentialsCommand(String name) {
			super(name);
		}

		@Override
		public boolean execute(CommandSender sender, String commandLabel, String[] args) {
			if(commandExecutor != null) {
				// execute the command!
				commandExecutor.onCommand(sender, this, commandLabel, args);
			}
			return false;
		}

		public void setExecutor(CommandExecutor commandExecutor) {
			this.commandExecutor = commandExecutor;
		}
	}
	
	// keep track of our own command info
	public class CommandInfo {	
		public Command command = null;
		public Method method = null;
		public ArrayList<String> permissions = null;
	}
	
	// our registered commands and command descriptions (for help)
	protected HashMap<String, CommandInfo> registeredCommands = new HashMap<String, CommandInfo>();
	protected HashMap<String, String> aliasMapping = new HashMap<String, String>();
	
	// call this to load our commands
	public void loadCommands(ComponentManager componentManager) {
		// use reflection to get access to bukkit's command map
		try {			
			// grab our field
			final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			
			// make it accessible
			boolean accessible = commandMapField.isAccessible();
			commandMapField.setAccessible(true);
			
			// now get the actual command map
			CommandMap commandMap = (CommandMap)commandMapField.get(Bukkit.getServer());
			
			// register commands from the class manager
			HashMap<String, Component> registeredComponents = componentManager.getRegisteredComponents();
			for(String component: registeredComponents.keySet()) {
				// and register it's methods
				// but only if its not disabled
				if(!registeredComponents.get(component).disabled) {
					registerComponentCommands(commandMap, registeredComponents.get(component));
				}
			}
			
			// now register all our commands and aliases with bukkit
			final Field commandMapKnownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
			commandMapKnownCommandsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			HashMap<String, org.bukkit.command.Command> knownCommands = (HashMap<String, org.bukkit.command.Command>)commandMapKnownCommandsField.get(commandMap);
			for(String knownAlias: knownAliases) {
				// if it's already a bukkit command, overwrite it
				if(commandMap.getCommand(knownAlias) != null) {
					Logger.warning("Overwriting command '%s'!", knownAlias);
					commandMap.getCommand(knownAlias).unregister(commandMap);
				}
				
				// create an actual command, injecting it into the Bukkit commandMap
				EssentialsCommand essentialsCommand = new EssentialsCommand(knownAlias);
				/// manually overwrite the command
				knownCommands.put(knownAlias, essentialsCommand);
				// register our command map to our command
				essentialsCommand.register(commandMap);
				// set our new command's executor to be this class
				essentialsCommand.setExecutor(this);
			}
			
			// restore our commandMap to its former glory
			commandMapField.setAccessible(accessible);
		}
		catch(Exception e) {
			e.printStackTrace();
			Logger.error("Failed to load components / commands!");
		}
	}
	
	// utility function to ensure the function we're trying to load
	// only has basic parameter types
	private Boolean validParameterType(Class<?> type) {
		if(type == int.class) {
			return true;
		}
		else if(type == float.class) {
			return true;
		}
		else if(type == String.class) {
			return true;
		}
		else if(type == String[].class) {
			return true;
		}
		
		return false;
	}
	
	// create a custom command registration string that makes each one unique
	private String buildRegistrationString(CommandInfo ci) {
		String str = new String(ci.command.command());
		
		// add who can execute us
		if(ci.command.consoleOnly()) {
			str += ":c";
		}
		else if(ci.command.playerOnly()) {
			str += ":p";
		}
		else {
			str += ":b";
		}
		
		// now add our arguments
		Class<?>[] parameterTypes = ci.method.getParameterTypes();
		for(int i = 1; i < parameterTypes.length; i++) {
			if(parameterTypes[i] == int.class) {
				str += ":i";
			}
			else if(parameterTypes[i] == float.class) {
				str += ":f";
			}
			else if(parameterTypes[i] == String.class) {
				str += ":s";
			}
			else if(parameterTypes[i] == String[].class) {
				str += ":sa";
			}
		}
		
		return str;
	}
	
	// check to see if the command is already registered or not
	@SuppressWarnings("unused")
	private boolean commandIsRegistered(String command) {
		// check if we have it as an alias
		if(aliasMapping.containsKey(command)) {
			return true;
		}
		
		// no? Well maybe its the normal command then..
		for(String registration: registeredCommands.keySet()) {
			String[] parts = registration.split(":");
			if(parts[0].equals(command)) {
				return true;
			}
		}
		return false;
	}
	
	// go through a given class and register all the commands in it
	private void registerComponentCommands(CommandMap commandMap, Component component) {
		// get our class
		Class<?> cls = component.clazz;
		
		// loop through all our methods in the given class
		for(Method method: cls.getMethods()) {
			// make sure it has the "Command" annotation on it
			if(!method.isAnnotationPresent(Command.class)) {
				continue;
			}
			
			// ok, now make sure the command is static
			if(!Modifier.isStatic(method.getModifiers())) {
				Logger.warning("failed to register command: " + method.getName() + " (not static)");
				continue;
			}
			
			// get the command
			Command command = method.getAnnotation(Command.class);
			
			// create a command info
			CommandInfo ci = new CommandInfo();
			ci.command = command;
			ci.method = method;
			
			// make sure it has an appropriate return value
			if(method.getReturnType() != boolean.class){
				Logger.warning("failed to register command: " + method.getName() + " (doesn't return boolean)");
				continue;
			}
			
			// figure out the arguments
			Class<?>[] parameterTypes = ci.method.getParameterTypes();
			
			// make sure there is at least argument and it is a command sender
			if(parameterTypes.length < 1) {
				Logger.warning("failed to register command: " + method.getName() + " (doesn't have a CommandSender argument as arg0)");
				continue;
			}
			
			// now loop through all the parameter types
			// and fill in the arguments string
			boolean valid = true;
			for(int i = 1; i < parameterTypes.length && valid; i++) {
				if(!validParameterType(parameterTypes[i])) {
					// we don't know what this is!
					Logger.warning("failed to register command method: " + method.getName() + " (unhandle-able parameter type: " + parameterTypes[i].getName() + ")");
					valid = false;
				}
			}
			// check if we need to skip this method
			if(!valid) {
				continue;
			}
			
			// get the list of permissions			
			// and add it to our commandinfo
			String[] permissions = command.permissions();
			if(permissions.length > 0) {
				ci.permissions = new ArrayList<String>();
				
				// add all of our permissions
				// (we will match ANY of these)
				for(int i = 0; i < permissions.length; i++) {
					ci.permissions.add(component.componentInfo.permsSettingsPrefix() + "." + permissions[i]);
				}
			}
			
			// check to see if we have a player / console only annotation
			if(command.playerOnly() && command.consoleOnly()) {
				// we can't have both!
				Logger.warning("failed to register command method: " + method.getName() + " (can't have BOTH ConsoleOnly and PlayerOnly attributes!)");
				continue;
			}
			
			// make sure we're not repeating a command here
			if(registeredCommands.containsKey(ci.command.command())) {
				Logger.warning("failed to register command: " + ci.command.command() + " (command exists in another component)");
				continue;
			}
			
			// build a registration string
			String registrationString = buildRegistrationString(ci);
			
			// use a registration string to register it
			registeredCommands.put(registrationString, ci);
			
			// build an array of all our aliases (including the main command) for this command
			ArrayList<String> commandAndAliases = new ArrayList<String>();
			commandAndAliases.add(ci.command.command());
			for(int i = 0; i < ci.command.aliases().length; i++) {
				commandAndAliases.add(ci.command.aliases()[i]);
			}
			
			// register our command AND aliases with bukkit
			for(int i = 0; i < commandAndAliases.size(); i++) {
				// make sure its not disabled
				boolean disabled = false;
				for(String cmd: component.disabledCommands) {
					if(cmd.equalsIgnoreCase(commandAndAliases.get(i))) {
						disabled = true;
						break;
					}
				}
				if(disabled) {
					//Logger.debug("Command alias '%s' disabled!", commandAndAliases.get(i));
					continue;
				}
				
				// add this is as a known alias
				knownAliases.add(commandAndAliases.get(i));
				
				// yay!
				if(i == 0) {
					//MCNSAEssentials.log("&aregistered command: " + registrationString);
				}
				else {
					// add it to the alias mapping
					aliasMapping.put(commandAndAliases.get(i), commandAndAliases.get(0));
					
					//MCNSAEssentials.log("\t&aregistered alias `" + commandAndAliases.get(i) + "' for: " + registrationString);
				}
			}
		}
	}

	@Override
	// here is where we actually handle commands
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
		// handle aliases
		if(aliasMapping.containsKey(label)) {
			label = aliasMapping.get(label);
		}
		
		/*for(String registrationToken: registeredCommands.keySet()) {
			MCNSAEssentials.debug("possible registration: " + registrationToken);
		}*/
		
		// find all our possibilities
		String lastFailMessage = "";
		for(String registrationToken: registeredCommands.keySet()) {
			//MCNSAEssentials.debug("testing " + registrationToken + " against command (" + label + ")");
			String[] registrationParts = registrationToken.split(":");

			if(!registrationParts[0].equals(label)) {
				// nope!
				//MCNSAEssentials.debug("failed " + registrationToken + ": not correct command (" + label + ")");
				continue;
			}
			
			// match the command sender type
			if(registeredCommands.get(registrationToken).command.playerOnly() && !registrationParts[1].equals("p")) {
				// nope, not this one!
				//MCNSAEssentials.debug("failed " + registrationToken + ": player only and not a player");
				continue;
			} 
			else if(registeredCommands.get(registrationToken).command.consoleOnly() && !registrationParts[1].equals("c")) {
				// nope, not this one!
				//MCNSAEssentials.debug("failed " + registrationToken + ": console only and not a console");
				continue;
			}
			
			// ok, this one matches the name and who can execute it
			Class<?>[] params = registeredCommands.get(registrationToken).method.getParameterTypes();
			
			// check the number of arguments
			/*if((params.length - 1) != args.length) {
				continue;
			}*/
			
			// check the arguments one by one
			Object[] arguments = new Object[params.length];
			
			// fill in our CommandSender
			arguments[0] = sender;
			
			// skip the CommandSender
			boolean possible = true;
			for(int i = 1; i < params.length && possible; i++) {
				// parse ints next
				if(params[i].equals(int.class)) {
					try {
						int pi = Integer.parseInt(args[i - 1]);
						arguments[i] = pi;
					}
					catch(Exception e) {
						// we didn't supply an int..
						possible = false;
					}
				}
				// floats next
				else if(params[i].equals(float.class)) {
					try {
						float pi = Float.parseFloat(args[i - 1]);
						arguments[i] = pi;
					}
					catch(Exception e) {
						// we didn't supply an int..
						possible = false;
					}
				}
				// strings now
				else if(params[i].equals(String.class)) {
					try {
						arguments[i] = args[i - 1];
					}
					catch(Exception e) {
						possible = false;
					}
				}
				// string array
				else if(params[i].equals(String[].class)) {
					try {
						String[] argsArray = new String[args.length - (i - 1)];
						
						// fill in the array
						for(int j = 0; j < argsArray.length; j++) {
							argsArray[j] = args[i - 1 + j];
						}
						
						// store the argument
						arguments[i] = argsArray;
					}
					catch(Exception e) {
						possible = false;
					}
				}
				// something else?
				else {
					possible = false;
				}
			}
			
			if(possible) {
				// we found a possible function!
				CommandInfo ci = registeredCommands.get(registrationToken);
				
				// check permissions first
				if(ci.permissions != null && (sender instanceof Player)) {
					boolean hasPermission = false;
					// loop through all the permissions and see if we have at least one
					for(Iterator<String> it = ci.permissions.iterator(); it.hasNext();) {
						if(PermissionsManager.playerHasPermission((Player)sender, it.next())) {
							hasPermission = true;
							break;
						}
					}
					
					// check to see if we have permission
					if(!hasPermission) {
						/*ColourHandler.sendMessage(sender, "&cSorry, you don't have permission to do that!");
						return false;*/
						lastFailMessage = "&cSorry, you don't have permission to do that!";
						continue;
					}
				}
				
				// make sure we have the right person trying to do the command
				if(ci.command.playerOnly() && !(sender instanceof Player)) {
					lastFailMessage = "&cSorry, that command is for players only";
					continue;
				}
				else if(ci.command.consoleOnly() && (sender instanceof Player)) {
					lastFailMessage = "&cSorry, that command is for the console only";
					continue;
				}
				
				// finally, call the method
				// the null is because the method must be static
				try {
					boolean result = (Boolean)ci.method.invoke(null, arguments);
					return result;
				}
				catch(Exception e) {					
					if(e.getCause() instanceof EssentialsCommandException) {
						ColourHandler.sendMessage(sender, "&c" + e.getCause().getMessage());
						return true;
					}
					else {
						ColourHandler.sendMessage(sender, "&cSomething went wrong! Alert an administrator!");
						Logger.error("failed to execute command: " + label + " (" + e.getMessage() + ")");
						e.printStackTrace();
						return false;
					}
				}
			}
		}
		
		// if we got here, we couldn't find a matching function
		if(lastFailMessage.equals("")) {
			ColourHandler.sendMessage(sender, "&cInvalid command! Type /help for some help!");
		}
		else {
			ColourHandler.sendMessage(sender, lastFailMessage);
		}
		return false;
	}
}
