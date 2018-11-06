package model;

public class SessionRiskResponse {

    private Boolean authenticated;
    private String advice;
    private int score;

    public SessionRiskResponse(Boolean authenticated, String advice) {
        this.authenticated = authenticated;
        this.advice = advice;
    }

    public SessionRiskResponse(Boolean authenticated, String advice, int score) {
        this.authenticated = authenticated;
        this.advice = advice;
        this.score = score;
    }

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public String getAdvice() {
        return advice;
    }

    public int getScore() {
        return score;
    }
}