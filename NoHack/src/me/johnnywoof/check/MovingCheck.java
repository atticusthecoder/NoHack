package me.johnnywoof.check;

import java.util.HashMap;

import me.johnnywoof.CheckType;
import me.johnnywoof.NoHack;
import me.johnnywoof.util.Utils;
import me.johnnywoof.util.XYZ;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MovingCheck {

	private final HashMap<String, MovePacketData> movepackets = new HashMap<String, MovePacketData>();
	public final HashMap<String, XYZ> lastGround = new HashMap<String, XYZ>();
	
	public int checkMove(NoHack nh, Player p, Location from, Location to){
		
		double yd = Math.abs((from.getY() - to.getY()));//Vertical speed
		boolean up = ((to.getY() - from.getY()) > 0);//Moving up?
		double md = this.getXZDistance(from.getX(), to.getX(), from.getZ(), to.getZ());//Horizontal speed
		@SuppressWarnings("deprecation")
		boolean onground = p.isOnGround();//Yeah...I'm aware how clients can send "always true" booleans.
		XYZ lg = this.lastGround.get(p.getName());
		if(lg == null){
			lg = new XYZ(from);
		}
		//double dis = lg.toLocation().distanceSquared(to);

		//Minor preformance increase
		if(to.getBlockX() != from.getBlockX() || to.getBlockY() != from.getBlockY() || to.getBlockZ() != from.getBlockZ()){
			
			//TODO Add crap here
			
		}
		
		if(to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()){
			
			int tid = this.checkTimer(nh, p);
			
			if(tid != 0){
				return tid;
			}
			
			if(yd != 0){//Actually move on the y axis
				
				if(up){
					
					if(yd > this.getMaxVertical(p)){//Moving up only
						
						int id = nh.raiseViolationLevel(p.getName(), CheckType.VERTICAL_SPEED);
						
						if(id != 0){
							
							Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed Vertical Speed! Speed was " + yd + ". VL " + id);
							
						}
						return 1;
						
					}
					
				}
				
			}
			
			if(md != 0){
				
				if(md > this.getMaxHorizontal(p)){
					
					int id = nh.raiseViolationLevel(p.getName(), CheckType.HORIZONTAL_SPEED);
					
					if(id != 0){
						
						Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed Horizontal Speed! Speed was " + md + ". VL " + id);
						
					}
					return 1;
					
				}
				
			}
			
			double ydis = Math.abs(lg.y - to.getY());
			
			//Bukkit.broadcastMessage((to.getY() - lg.y) + ":" + p.getFallDistance());
			
			if(!up && yd > 0.25 && onground){ //Falling while onground? I DON'T THINK SO
				
				int id = nh.raiseViolationLevel(p.getName(), CheckType.NOFALL);
					
				if(id != 0){
						
					Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed NoFall! Tried to fall while stating on the ground. VL " + id);
						
				}
				
				return 4;
				
			}
			
			if(onground){
				
				this.lastGround.put(p.getName(), new XYZ(from));
				
			}else{
				
				//TODO Add fly check when ground = true
				if(!p.isFlying()){//Ignore users that are allowed to fly. Doesn't count for the hack fly!
					
					if(up){
						
						if(ydis > 1.35){
							
							int id = nh.raiseViolationLevel(p.getName(), CheckType.FLY);
							
							if(id != 0){
								
								Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed Fly! Height was " + ydis + ". VL " + id);
								
							}
							return 1;
							
						}
						
					}
					
					//TODO Make this work
					//double xzd = this.getXZDistance(lg.x, to.getX(), lg.z, to.getZ());
					
					
					
				}
				
			}

		}
		
		return 0;
		
	}
	
	private double getXZDistance(double x1, double x2, double z1, double z2){
		
		double a1 = (x2 - x1), a2 = (z2 - z1);
		
		return ((a1 * (a1)) + (a2 * a2));
		
	}
	
	private int checkTimer(NoHack nh, Player p){
		MovePacketData mpd = this.movepackets.get(p.getName());
		
		if(mpd == null){
			
			mpd = new MovePacketData(System.currentTimeMillis(), 1);
			
		}
		
		mpd.setAmount(mpd.getAmount() + 1);
		
		if((System.currentTimeMillis() - mpd.getTimeStart()) >= 1000){
			
			if(mpd.getAmount() >= (23 + Math.round(Utils.getPing(p) / 100))){
				
				if(mpd.getAmount() == mpd.lastamount + 1){
					
					if((System.currentTimeMillis() - mpd.getTimeStart()) >= 5000){
						
						mpd.reset();
						
					}else{
						
						mpd.lastamount = mpd.getAmount() + 1;
						int id = nh.raiseViolationLevel(p.getName(), CheckType.TIMER);
						
						if(id != 0){
							
							Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed Timer! Sent more packets than expected. VL " + id);
							
						}
						return 1;
						
					}
					
				}else{
				
					mpd.lastamount = mpd.getAmount();
					int id = nh.raiseViolationLevel(p.getName(), CheckType.TIMER);
					
					if(id != 0){
						
						Utils.messageAdmins(ChatColor.YELLOW + "" + p.getName() + "" + ChatColor.GREEN + " failed Timer! Sent more packets than expected. VL " + id);
						
					}
					return 1;
				
				}
				
			}else{
				
				mpd.reset();
				
			}
			
		}
		
		this.movepackets.put(p.getName(), mpd);
		return 0;
		
	}
	
	private double getMaxHorizontal(Player p){
		
		double d = 0;
		
		if(p.isFlying() && p.getAllowFlight()){
		
			d = 0.305;
			
		}else{
			
			if(p.isSprinting()){
				
				d = 0.788;
				
			}else if(p.isSneaking()){
				
				if(p.getAllowFlight()){
					
					d = 0.0053;
					
				}else{
				
					d = 0.0042;
				
				}
				
			}else if(p.isBlocking()){
				
				d = 0.0075;
				
			}else{
				
				d = 0.466;
			
			}
		
		}
		
		double cd = ((p.getVelocity().getX() + p.getVelocity().getZ()) * 5);
		
		if(cd > 0){
		
			d = cd + d;
		
		}
		
		return d;
		
	}
	
	private double getMaxVertical(Player p){
		
		double d = 0;
		
		if(p.hasPotionEffect(PotionEffectType.JUMP)){
			
			d = ((this.getPotionEffectLevel(p, PotionEffectType.JUMP)) * 0.17) + 0.25;
			
		}else if(p.getVelocity().getY() > 0){
			
			d = p.getVelocity().getY() + 0.5;
			
		}else{
		
			d = 0.5;
		
		}
		
		return d;
		
	}
	
	private int getPotionEffectLevel(Player p, PotionEffectType pet){
		
		for(PotionEffect pe : p.getActivePotionEffects()){

			if(pe.getType().getName().equals(pet.getName())){
				
				return pe.getAmplifier() + 1;
				
			}
			
		}
		
		return 0;
		
	}
	
	public boolean isReallyOnGround(Location loc)
	{
		if(loc.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()){
			double dv = loc.getY() % 1;
			if(dv == 0){
				
				return true;
				
			}else{
				
				//Bukkit.broadcastMessage(dv + "");
				
				if(dv <= 0.1){
					
					return true;
				
				}else{
						
					return false;
					
				}
				
			}
		}else{
			
			return false;
		
		}
		
	}
	
}
