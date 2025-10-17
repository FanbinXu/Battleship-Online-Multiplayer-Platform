package app.battleship.model;

public enum ShipKind {
    CARRIER_5(5),
    BATTLESHIP_4(4),
    CRUISER_3(3),
    SUBMARINE_3(3),
    DESTROYER_2(2);
    
    private final int length;
    
    ShipKind(int length) {
        this.length = length;
    }
    
    public int getLength() {
        return length;
    }
}



