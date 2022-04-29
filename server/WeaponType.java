package server;

public enum WeaponType {
	FIST(10, 60, -1, "Fist"),
	NUNCHUCKS(30, 75, 20, "Nunchucks"),
	BAT(100, 80, 15, "Bat"),
	KNIFE(150, 60, 15, "Knife"),
	SWORD(150, 80, 10, "Sword"),
	MACE(200, 50, 2, "Mace"),
	SLEDGEHAMMER(250, 40, 2, "Sledge"),
	LASERSWORD(250, 100, 1, "Laser Sword");
	
	private int damage;
	private int accuracy;
	private int uses;
	private String name;
	
	/* damage -- how much health it takes away
	 * accuracy -- likelihood of landing a hit
	 * uses -- how many times it can be used in one hold*/
	WeaponType(int damage, int accuracy, int uses, String name) {
		this.damage = damage;
		this.accuracy = accuracy;
		this.uses = uses;
		this.name = name;
	}
	
	public int getDamage() {
		return damage;
	}
	
	public int getAccuracy() {
		return accuracy;
	}
	
	public int getUses() {
		return uses;
	}
	
	public String getName() {
		return name;
	}
}
