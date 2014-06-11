package me.johnnywoof.check.fight;

import me.johnnywoof.Setting;
import me.johnnywoof.Variables;
import me.johnnywoof.check.Check;
import me.johnnywoof.check.CheckType;
import me.johnnywoof.check.DetectionType;
import me.johnnywoof.event.ViolationTriggeredEvent;
import me.johnnywoof.util.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FightImpossible extends Check{

	public FightImpossible(Variables vars, CheckType ct) {
		super(vars, ct, DetectionType.FIGHT);
	}
	
	@Override
	public int runAttackCheck(Player p, LivingEntity e, long ls){
		
		if(p.isBlocking() || p.isSleeping() || p.isDead() || p.getEntityId() == e.getEntityId()){
			
			int id = this.vars.raiseViolationLevel(CheckType.IMPOSSIBLE, p);
			
			ViolationTriggeredEvent vte = new ViolationTriggeredEvent(id, CheckType.IMPOSSIBLE, p);
			
			Bukkit.getServer().getPluginManager().callEvent(vte);
			
			if(!vte.isCancelled()){
				
				if(id != 0){
					
					String message = Setting.impossibleattack;
					
					message = message.replaceAll(".name.", ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN);
					message = message.replaceAll(".vl.", id + "");

					Utils.messageAdmins(message);
					
				}
				return 1;
			
			}
			
		}else{
			
			//Kind of performance heavy, only check it when they get the damage
			if(((CraftLivingEntity) e).getHandle().hurtTicks <= 3){
				
				if(!Utils.canReallySeeEntity(p, e)){
					
					int id = this.vars.raiseViolationLevel(CheckType.VISIBLE, p);
					
					ViolationTriggeredEvent vte = new ViolationTriggeredEvent(id, CheckType.VISIBLE, p);
					
					Bukkit.getServer().getPluginManager().callEvent(vte);
					
					if(!vte.isCancelled()){
						
						if(id != 0){
							
							String message = Setting.fightvisiblemes;
							
							message = message.replaceAll(".name.", ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN);
							message = message.replaceAll(".vl.", id + "");

							Utils.messageAdmins(message);
							
						}
						return 1;
					
					}
					
				}
				
				/*LivingEntity t = Utils.getTarget(p);
				
				if((t == null) ? true : t.getUniqueId() != e.getUniqueId()){
					
					int id = this.vars.raiseViolationLevel(CheckType.VISIBLE, p);
					
					ViolationTriggeredEvent vte = new ViolationTriggeredEvent(id, CheckType.VISIBLE, p);
					
					Bukkit.getServer().getPluginManager().callEvent(vte);
					
					if(!vte.isCancelled()){
						
						if(id != 0){
							
							String message = Setting.fightvisiblemes;
							
							message = message.replaceAll(".name.", ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN);
							message = message.replaceAll(".vl.", id + "");

							Utils.messageAdmins(message);
							
						}
						return 1;
					
					}
					
				}
				
				t = null;*/
				
			}
			
		}
		
		return 0;
		
	}

}
