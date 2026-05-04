package id.icapps.savera.model;

public class LeaderboardItem {
    private final int rank;
    private final String fullname;
    private final String position;
    private final String averageSleep;
    private final String days;
    private final int photo;

    public LeaderboardItem(int rank, String fullname, String position, String averageSleep, String days, int photo) {
        this.rank = rank;
        this.fullname = fullname;
        this.position = position;
        this.averageSleep = averageSleep;
        this.days = days;
        this.photo = photo;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return fullname;
    }

    public String getPosition() {
        return position;
    }

    public String getAverageSleep() {
        return averageSleep;
    }

    public String getDays() {
        return days;
    }

    public int getPhoto() {
        return photo;
    }
}
