package io.agora.openlive.activities.pojo;

import java.io.Serializable;

public class Question implements Serializable {

    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer count;
    private Boolean isShowQuiz;


    public Question() {

    }

    /*public Question(String question, String optionA, String optionB, String optionC, String optionD) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
    }*/

    public Question(String question, String optionA, String optionB, String optionC, String optionD, Integer count, Boolean isShowQuiz) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.count = count;
        this.isShowQuiz = isShowQuiz;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Boolean getShowQuiz() {
        return isShowQuiz;
    }

    public void setShowQuiz(Boolean showQuiz) {
        isShowQuiz = showQuiz;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

}
