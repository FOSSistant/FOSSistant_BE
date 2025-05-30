package Capstone.FOSSistant.global.domain.enums;

public enum Tag {
    EASY,
    MEDIUM,
    HARD,
    MISC;
    public String toLowerCase() {
        return this.name().toLowerCase();
    }
}
