package app.battleship.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class BattleshipProperties {
    
    private Jwt jwt = new Jwt();
    private OpenAI openai = new OpenAI();
    private Room room = new Room();
    private Turn turn = new Turn();
    private Reconnect reconnect = new Reconnect();
    
    public static class Jwt {
        private String secret = "please_change_me_this_is_a_very_long_secret_key_for_jwt_signing";
        private long expiration = 86400;
        
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
    }
    
    public static class OpenAI {
        private Api api = new Api();
        
        public Api getApi() { return api; }
        public void setApi(Api api) { this.api = api; }
        
        public static class Api {
            private String key = "";
            
            public String getKey() { return key; }
            public void setKey(String key) { this.key = key; }
        }
    }
    
    public static class Room {
        private Empty empty = new Empty();
        private Cleanup cleanup = new Cleanup();
        
        public Empty getEmpty() { return empty; }
        public void setEmpty(Empty empty) { this.empty = empty; }
        public Cleanup getCleanup() { return cleanup; }
        public void setCleanup(Cleanup cleanup) { this.cleanup = cleanup; }
        
        public static class Empty {
            private long ttl = 60;
            
            public long getTtl() { return ttl; }
            public void setTtl(long ttl) { this.ttl = ttl; }
        }
        
        public static class Cleanup {
            private long interval = 30000;
            
            public long getInterval() { return interval; }
            public void setInterval(long interval) { this.interval = interval; }
        }
    }
    
    public static class Turn {
        private Timeout timeout = new Timeout();
        
        public Timeout getTimeout() { return timeout; }
        public void setTimeout(Timeout timeout) { this.timeout = timeout; }
        
        public static class Timeout {
            private long sec = 30;
            
            public long getSec() { return sec; }
            public void setSec(long sec) { this.sec = sec; }
        }
    }
    
    public static class Reconnect {
        private Grace grace = new Grace();
        
        public Grace getGrace() { return grace; }
        public void setGrace(Grace grace) { this.grace = grace; }
        
        public static class Grace {
            private long sec = 60;
            
            public long getSec() { return sec; }
            public void setSec(long sec) { this.sec = sec; }
        }
    }
    
    // Getters and setters
    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public OpenAI getOpenai() { return openai; }
    public void setOpenai(OpenAI openai) { this.openai = openai; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Turn getTurn() { return turn; }
    public void setTurn(Turn turn) { this.turn = turn; }
    public Reconnect getReconnect() { return reconnect; }
    public void setReconnect(Reconnect reconnect) { this.reconnect = reconnect; }
}

