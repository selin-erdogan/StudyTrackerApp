public class Quiz {
    private int id;
    private int subjectId;
    private double score;
    private double maxScore;
    private String date;

    public Quiz(int id, int subjectId, double score, double maxScore, String date) {
        this.id = id;
        this.subjectId = subjectId;
        this.score = score;
        this.maxScore = maxScore;
        this.date = date;
    }

    // TableView için Getter metodları
    public int getId() { return id; }
    public int getSubjectId() { return subjectId; }
    public double getScore() { return score; }
    public double getMaxScore() { return maxScore; }
    public String getDate() { return date; }
}