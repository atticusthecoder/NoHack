package me.johnnywoof;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import me.johnnywoof.check.Check;
import me.johnnywoof.check.CheckType;
import me.johnnywoof.check.CustomCheck;
import me.johnnywoof.check.DetectionType;
import me.johnnywoof.event.ViolationChangedEvent;
import me.johnnywoof.event.ViolationTriggeredEvent;
import me.johnnywoof.util.MoveData;
import me.johnnywoof.util.Utils;
import me.johnnywoof.util.XYZ;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class NoHackListener implements Listener {

	private NoHack nh;
	
	private CustomCheck cc;
	
	private final HashMap<String, Long> lastHealhed = new HashMap<String, Long>();
	private final HashMap<String, String> lastViolation = new HashMap<String, String>();
	
	public NoHackListener(NoHack nh){
		
		this.nh = nh;
		this.cc = new CustomCheck();
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onMountExit(VehicleExitEvent event){
		
		if(event.getExited() instanceof Player){
		
			Player p = (Player) event.getExited();
			
			MoveData md = nh.vars.getMoveData(p.getName());
			
			md.lastmounting = System.currentTimeMillis();
			
			nh.vars.setMoveData(p.getName(), md);
		
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onMount(VehicleEnterEvent event){
		
		if(event.getEntered() instanceof Player){
		
			Player p = (Player) event.getEntered();
			
			MoveData md = nh.vars.getMoveData(p.getName());
			
			md.lastmounting = System.currentTimeMillis();
			
			nh.vars.setMoveData(p.getName(), md);
		
		}
		
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)//Better make sure it's not canceled!
	public void onVelocity(PlayerVelocityEvent event){
		
		if(event.getVelocity().getX() != 0 || event.getVelocity().getY() != 0 || event.getVelocity().getZ() != 0){
		
			MoveData md = nh.vars.getMoveData(event.getPlayer().getName());
			
			double vy = Math.abs(event.getVelocity().getY());
			double vx = Math.abs(event.getVelocity().getX());
			double vz = Math.abs(event.getVelocity().getZ());
			
			md.yda = (((vy * 24.4)));
				
			md.velexpirey = (long) (System.currentTimeMillis() + (md.yda * 51));
			
			md.velexpirex = (long) (System.currentTimeMillis() + ((((md.mda == 0) ? 1 : md.mda) * (md.yda * 4)) * 115) * 2.5);
			
			md.mda = (vx + vz) * 16;
			
			nh.vars.setMoveData(event.getPlayer().getName(), md);
		
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)//Permission system bypass
	public void onViolationTriggeredEvent(ViolationTriggeredEvent event){
		
		if(Setting.ignorenpc){
			
			if(event.getPlayer().hasMetadata("NPC")){
				
				event.setCancelled(true);
				return;
				
			}
			
		}
		
		if(event.getPlayer().hasPermission("nohack.bypass." + event.getCheckType().toString().toLowerCase())){
			
			//event.setCancelled(true);
			//return;
			
		}
		
		//TODO Re-add this back to the }else{ thingy
		
		long diff = 0;
		
		CheckType lf = null;
		
		if(this.lastViolation.containsKey(event.getPlayer().getName())){
			
			String[] s = this.lastViolation.get(event.getPlayer().getName()).split(":");
			
			diff = System.currentTimeMillis() - Long.parseLong(s[1]);
			
			lf = CheckType.valueOf(s[0]);
			
		}
		
		if(event.getCheckType() != CheckType.VERTICAL_SPEED && event.getCheckType() != CheckType.AUTOSOUP){
			
			if(lf == null){
				
				event.setCancelled(true);
				
			}else{
			
				if(diff > 1000){
					
					//"Forgive" the player
					event.setCancelled(true);
					
				}
			
			}
			
		}
		
		//Prevents abuse of checks to slow down server
		if(event.getCheckType() == CheckType.FAST_INTERACT || event.getCheckType() == CheckType.SPEED_BREAK){
		
			if(diff <= 2000 && event.getNewLevel() > 35){
				
				event.getPlayer().kickPlayer(ChatColor.RED + "Detected illegal activity! Are you hacking?");
				
				this.nh.vars.setDeniedLogin(event.getPlayer(), (System.currentTimeMillis() + 10000), "Kicked for illegal activities - Interacting too fast.");
				
				event.setNewLevel(0);
				
			}
			
		}else if(event.getCheckType() == CheckType.AUTOSOUP){
			
			if(Setting.autoban){
				
				this.nh.vars.setDeniedLogin(event.getPlayer(), (System.currentTimeMillis() + (Setting.autobantime * 1000)), "AutoBanned - Using AutoSoup");
				
				event.getPlayer().kickPlayer(ChatColor.RED + "You've been autobanned for " + Setting.autobantime + " seconds!\n\nReason: Using AutoSoup");
				
				Utils.messageAdmins(event.getPlayer().getName() + " has been autobanned for using AutoSoup for " + Setting.autobantime + " seconds!");
				
			}
			
		}
		
		this.lastViolation.put(event.getPlayer().getName(), event.getCheckType().toString() + ":" + System.currentTimeMillis());
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onLog(ViolationChangedEvent event){
		
		//TODO Configurable logging
		
		File f = new File("hack_logs.txt");
		
		try{
		
			if(!f.exists()){
				
				f.createNewFile();
				
			}
			
			PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
			
			w.println(event.getPlayer().getName() + " failed " + event.getCheckType().toString() + ". VL " + event.getNewLevel() + " TPS " + NoHack.tps);
			
			w.close();
		
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onSneak(PlayerToggleSneakEvent event){
		
		MoveData md = nh.vars.getMoveData(event.getPlayer().getName());
		
		md.sneaktime = System.currentTimeMillis();
		
		md.wassneaking = !event.isSneaking();
		
		nh.vars.setMoveData(event.getPlayer().getName(), md);
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onSprint(PlayerToggleSprintEvent event){
		
		MoveData md = nh.vars.getMoveData(event.getPlayer().getName());
		
		md.sprinttime = System.currentTimeMillis();
		
		md.wassprinting = !event.isSprinting();
		
		nh.vars.setMoveData(event.getPlayer().getName(), md);
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent event){
		
		if(event.getWhoClicked().getType() == EntityType.PLAYER){
			
			Player p = (Player) event.getWhoClicked();
			
			for(Check c : nh.getChecks()){
				
				if(c.getDetectType() == DetectionType.INVENTORY){
					
					if(c.runInventoryCheck(p, event.getInventory(), event.getAction(), event) != 0){
						
						event.setCancelled(true);
						break;
						
					}
					
				}
				
			}
			
			p = null;
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityRegainHealthEvent(EntityRegainHealthEvent event){
		
		if(event.getRegainReason() == RegainReason.SATIATED){
			
			if(event.getEntity() instanceof Player){
			
				Player p = (Player) event.getEntity();
				
				long diff = 1000;
				if(this.lastHealhed.containsKey(p.getName())){
					diff = (System.currentTimeMillis() - this.lastHealhed.get(p.getName()));
				}
				
				this.lastHealhed.put(p.getName(), System.currentTimeMillis());
				
				if(diff <= 3800){
					
					int id = nh.vars.raiseViolationLevel(CheckType.GOD_MODE, p);
					
					ViolationTriggeredEvent vte = new ViolationTriggeredEvent(id, CheckType.GOD_MODE, p);
					
					nh.getServer().getPluginManager().callEvent(vte);
					
					if(!vte.isCancelled()){
					
						if(id != 0){
							
							Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed GodMode! Tried to regain health too fast. Diff " + diff + " VL " + id);
							
						}
						event.setCancelled(true);
					
					}
					
				}
				
				p = null;
				
			}
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerItemConsumeEvent(PlayerItemConsumeEvent event){
		
		if(cc.checkFastEat(event.getPlayer())){
			
			event.setCancelled(true);
			
		}
		
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event){
		
		if(event.hasItem()){
			
			if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
				
				Material m = event.getItem().getType();
				
				if(m == Material.BOW){
					
					cc.onStartingShoot(event.getPlayer());
					
				}else if(m == Material.WOOD_SWORD || m == Material.STONE_SWORD || m == Material.GOLD_SWORD || m == Material.IRON_SWORD || m == Material.DIAMOND_SWORD){
					
					MoveData md = nh.vars.getMoveData(event.getPlayer().getName());
					
					md.blocktime = System.currentTimeMillis();
					
					nh.vars.setMoveData(event.getPlayer().getName(), md);
					
				}
				
				//TODO Add fasteat
				
			}
			
		}
		
		if(event.getAction() != Action.PHYSICAL){
		
			for(Check c : nh.getChecks()){
				
				if(c.getDetectType() == DetectionType.INTERACT){
					
					int id = c.runInteractCheck(event.getPlayer(), event);
					
					if(id != 0){
						
						event.setCancelled(true);
						break;
						
					}
					
				}
				
			}
		
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onLogin(PlayerLoginEvent event){
		
		//Special mod-disable codes
		event.getPlayer().sendMessage("�3 �9 �2 �0 �0 �2");
		event.getPlayer().sendMessage("�3 �9 �2 �0 �0 �1");
		event.getPlayer().sendMessage("�3 �9 �2 �0 �0 �3");
		event.getPlayer().sendMessage("�f �f �2 �0 �4 �8");
		event.getPlayer().sendMessage("�f �f �4 �0 �9 �6");
		event.getPlayer().sendMessage("�f �f �1 �0 �2 �4");
		event.getPlayer().sendMessage("�0�0�1�e�f");
		event.getPlayer().sendMessage("�0�0�2�3�4�5�6�7�e�f");
		event.getPlayer().sendMessage("�0�0�1�f�e");
		event.getPlayer().sendMessage("�0�0�2�f�e");
		event.getPlayer().sendMessage("�0�0�3�4�5�6�7�8�f�e");
		event.getPlayer().sendMessage("�0�1�0�1�2�f�f");
		event.getPlayer().sendMessage("�0�1�3�4�f�f");
		event.getPlayer().sendMessage("�0�1�5�f�f");
		event.getPlayer().sendMessage("�0�1�6�f�f");
		event.getPlayer().sendMessage("�0�1�8�9�a�b�f�f");
		event.getPlayer().sendMessage("�0�1�7�f�f");
		
		String[] s = nh.vars.getDeniedData(event.getPlayer());
		
		if(s != null){
		
			long fut = Long.parseLong(s[0]);
				
			if(fut > System.currentTimeMillis()){
				
				event.disallow(Result.KICK_OTHER, "Please wait " + Math.round((fut - System.currentTimeMillis()) / 1000) + " seconds before joining again.\nReason: " + s[1]);
				
			}else{
					
				nh.vars.removeDeniedLogin(event.getPlayer().getUniqueId());
					
			}
		
		}
		
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	public void onFood(FoodLevelChangeEvent event){
		
		//TODO Remove this.....
		event.setFoodLevel(20);
		((Player) event.getEntity()).setSaturation(20);
		
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerAttack(EntityDamageByEntityEvent event){
		
		if(event.getDamager().getType() == EntityType.PLAYER){
			
			if(event.getEntity() instanceof LivingEntity){
				
				LivingEntity e = ((LivingEntity) event.getEntity());
				
				if((e.getHealth() > 0)){
				
					final Player p = ((Player) event.getDamager());
					
					long ls = nh.vars.getLastSwong(p.getName());
					
					for(Check c : nh.getChecks()){
						
						if(c.getDetectType() == DetectionType.FIGHT){
							
							if(c.runAttackCheck(p, e, ls) != 0){
								
								e.setNoDamageTicks(20);
								event.setCancelled(true);
								break;
								
							}
							
						}
						
					}
					
					//A fix for stupid bukkit not taking account of ctrl sprint
					//TODO Test this with craftbukkit and not spigot
					if(p.isSprinting()){
						
						nh.getServer().getPluginManager().callEvent(new PlayerToggleSprintEvent(p, false));
						
						nh.getServer().getScheduler().runTaskLater(this.nh, new Runnable(){

							@Override
							public void run() {
								
								p.setSprinting(true);
								
							}
							
						}, 1);
					
					}
				
				}
				
				e = null;
				
			}
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event){
		
		long ls = nh.vars.getLastSwong(event.getPlayer().getName());
		
		for(Check c : nh.getChecks()){
			
			if(c.getDetectType() == DetectionType.BLOCK){
				
				if(c.runBlockCheck(event.getPlayer(), event.getBlock(), null, ls, 0) != 0){
					
					event.setCancelled(true);
					break;
					
				}
				
			}
			
		}
		
	}

	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBedLeaveEvent(PlayerBedLeaveEvent event){
		
		if(event.getBed().getType() != Material.BED_BLOCK){
			
			event.getPlayer().kickPlayer("Go find a real bed!");
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBow(EntityShootBowEvent event){
		
		if(event.getEntity() instanceof Player){
			
			Player p = (Player) event.getEntity();
			
			if(cc.onShoot(p, event.getForce())){

				int id = nh.vars.raiseViolationLevel(CheckType.FAST_BOW, p);
				
				ViolationTriggeredEvent vte = new ViolationTriggeredEvent(id, CheckType.FAST_BOW, p);
				
				Bukkit.getServer().getPluginManager().callEvent(vte);
				
				if(!vte.isCancelled()){
				
					if(id != 0){
						
						Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed Fast Bow! Tried to fire a bow too fast. VL " + id);
						
					}
					event.setCancelled(true);
				
				}
				
			}
			
			p = null;
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onProjectile(ProjectileLaunchEvent event){
		
		if(event.getEntity().getShooter() != null){
			
			if(event.getEntity().getShooter() instanceof Player){
					
				Player p = (Player) event.getEntity().getShooter();
					
				if(cc.checkFastShoot(p)){
					
					int id = nh.vars.raiseViolationLevel(CheckType.FAST_THROW, p);
					
					ViolationTriggeredEvent vte = new ViolationTriggeredEvent(id, CheckType.FAST_THROW, p);
					
					Bukkit.getServer().getPluginManager().callEvent(vte);
					
					if(!vte.isCancelled()){
					
						if(id != 0){
							
							Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed Fast Throw! Tried to throw a projectile too fast. VL " + id);
							
						}
						event.setCancelled(true);
					
					}
					
				}
					
				p = null;
				
			}
			
		}
		
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBedLeaveEvent(PlayerBedEnterEvent event){
		
		if(event.getBed().getType() != Material.BED_BLOCK){
			
			event.getPlayer().kickPlayer("Go find a real bed!");
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event){
		
		for(Check c : nh.getChecks()){
			
			if(c.getDetectType() == DetectionType.CHAT){
				
				if(c.runChatCheck(event.getPlayer(), event.getMessage()) != 0){
					
					event.setCancelled(true);
					break;
					
				}
				
			}
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerAnimationEvent(org.bukkit.event.player.PlayerAnimationEvent event){
		
		if(event.getAnimationType() == PlayerAnimationType.ARM_SWING){
		
			nh.vars.updateLastSwong(event.getPlayer().getName());
		
		}
		
	}

	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event){
		
		if(event.getCause() != TeleportCause.UNKNOWN){
		
			MoveData md = this.nh.vars.getMoveData(event.getPlayer().getName());
			
			md.tptime = System.currentTimeMillis();
			
			this.nh.vars.setMoveData(event.getPlayer().getName(), md);
			
			if(Setting.debug){
			
				Bukkit.broadcastMessage("Logged teleport for " + event.getPlayer().getName() + ". Cause=" + event.getCause().toString());
			
			}
		
		}
		
	}
	
	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChangeWorlds(PlayerChangedWorldEvent event) {
		
		MoveData md = this.nh.vars.getMoveData(event.getPlayer().getName());
		
		md.tptime = System.currentTimeMillis();
		
		this.nh.vars.setMoveData(event.getPlayer().getName(), md);
		
		if(Setting.debug){
		
			Bukkit.broadcastMessage("Logged teleport for " + event.getPlayer().getName() + ". Cause=WORLD_CHANGE");
		
		}
		
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	public void onMove(PlayerMoveEvent event){
		
		if(event.getPlayer().isInsideVehicle() || event.getPlayer().isDead() || (event.getFrom().getX() == event.getTo().getX() && event.getFrom().getY() == event.getTo().getY() && event.getFrom().getZ() == event.getTo().getZ())){
			
			return;
			
		}
		
		MoveData mdd = nh.vars.getMoveData(event.getPlayer().getName());
		
		if(mdd.lastloc == null){
			
			mdd.lastloc = new XYZ(event.getPlayer().getLocation());
			
		}
		
		if((System.currentTimeMillis() - mdd.tptime) > 1000){
			
			/*mdd.setAmount(mdd.getAmount() + 1);
			
			if((System.currentTimeMillis() - mdd.getTimeStart()) >= 1000){
				
				int expected = (20 - NoHack.tps) + Setting.maxpacket + (Math.round(Math.abs(Utils.getPing(event.getPlayer()) / 100)));
				
				if(mdd.getAmount() > expected){
						
					int id = this.nh.vars.raiseViolationLevel(CheckType.TIMER, event.getPlayer());
						
					ViolationTriggeredEvent vte = new ViolationTriggeredEvent(id, CheckType.TIMER, event.getPlayer());
					
					Bukkit.getServer().getPluginManager().callEvent(vte);
					
					if(!vte.isCancelled()){
					
						if(id != 0){
								
							String message = Setting.timermes;
							
							message = message.replaceAll(".name.", ChatColor.YELLOW + "" + event.getPlayer().getName() + "" + ChatColor.GREEN);
							message = message.replaceAll(".vl.", id + "");
							message = message.replaceAll(".packets-sent.", mdd.getAmount() + "");
							message = message.replaceAll(".expected-packets.", expected + "");

							Utils.messageAdmins(message);
								
						}
						
						if(event.getPlayer().isInsideVehicle()){
							
							event.getPlayer().getVehicle().teleport(mdd.lastloc.toLocation(event.getPlayer().getLocation().getPitch(), event.getPlayer().getLocation().getYaw()), TeleportCause.UNKNOWN);
							
						}else{
						
							event.setTo(mdd.lastloc.toLocation(event.getPlayer().getLocation().getPitch(), event.getPlayer().getLocation().getYaw()));
						
						}
						mdd.reset(new XYZ(event.getPlayer().getLocation()));
						this.nh.vars.setMoveData(event.getPlayer().getName(), mdd);
						return;
					
					}
					
				}else{
					
					mdd.reset(new XYZ(event.getPlayer().getLocation()));
					
				}
				
			}
			
			this.nh.vars.setMoveData(event.getPlayer().getName(), mdd);*/
			
		}
		
		if((System.currentTimeMillis() - mdd.tptime) <= 1000){//Player teleported, don't check it
			
			return;
			
		}
		
		int id = 0;
		
		double vd = (event.getTo().getY() - event.getFrom().getY());
		
		double yd = Math.abs((event.getFrom().getY() - event.getTo().getY()));//Vertical speed
		boolean up = (vd > 0);//Moving up?
		
		if(!up){
			
			if(vd < 0){
				
				//Anti weepcraft
				//If someone has a more efficient method please tell me!
				if(String.valueOf(vd).length() <= 5){
					
					up = true;
					
					if(Setting.debug){
					
						Bukkit.broadcastMessage("Override up=true: length: " + String.valueOf(vd).length() + "; value=" + vd);
					
					}
					
				}
				
			}
			
		}
		
		double md = Utils.getXZDistance(event.getFrom().getX(), event.getTo().getX(), event.getFrom().getZ(), event.getTo().getZ());//Horizontal speed
		boolean inwater = ((CraftPlayer) event.getPlayer()).getHandle().inWater;
		boolean onladder = ((CraftPlayer) event.getPlayer()).getHandle().h_();//Near ladder? NMS ftw!
		
		XYZ lg = nh.vars.lastGround(event.getPlayer());
		
		for(Check c : nh.getChecks()){
			
			if(c.getDetectType() == DetectionType.MOVING){
				
				id = c.runMoveCheck(event.getPlayer(), event.getTo(), event.getFrom(), yd, md, mdd, up, inwater, onladder, lg);
				
				if(id > 0){
					
					break;
					
				}
				
			}
			
		}
		
		if(id == 1 || id == 2){
			
			event.setCancelled(true);
			if(event.getPlayer().isInsideVehicle()){
				
				event.getPlayer().getVehicle().teleport(new Location(event.getFrom().getWorld(), event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ(), event.getTo().getYaw(), event.getTo().getPitch()), TeleportCause.UNKNOWN);
				
			}else{
				
				event.getPlayer().teleport(new Location(event.getFrom().getWorld(), event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ(), event.getTo().getYaw(), event.getTo().getPitch()), TeleportCause.UNKNOWN);
				
			}
			
		}else if(id == 3){
			
			event.setTo(nh.vars.lastGround(event.getPlayer()).toLocation(event.getTo().getPitch(), event.getTo().getYaw()));
			
		}else if(id == 4){
			
			Location loc = nh.vars.lastGround(event.getPlayer()).toLocation(event.getTo().getPitch(), event.getTo().getYaw());
			
			double mmd = Double.MAX_VALUE;
			
			for(int x = loc.getBlockX() - 5; x < loc.getBlockX() + 5; x++){
				
				for(int y = loc.getBlockY() - 5; y < loc.getBlockY() + 5; y++){
				
					for(int z = loc.getBlockZ() - 5; z < loc.getBlockZ() + 5; z++){
						
						Block b = loc.getWorld().getBlockAt(x, y, z);
						
						if(b.getType().isSolid()){
							
							if(!b.getRelative(BlockFace.UP).getType().isSolid()){
								
								double d = b.getLocation().distanceSquared(loc);
								
								if(d < mmd){
									
									event.setTo(new Location(loc.getWorld(), x + 0.5, y + 1, z + 0.5, event.getTo().getYaw(), event.getTo().getPitch()));
									
									mmd = d;
								
								}
								
							}
							
						}
						
					}
				
				}
				
			}
			
		}
		
	}
	
}
