package io.agora.openlive.pojo;

import java.io.Serializable;

public class Question implements Serializable {

    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer count;
    private Boolean isShowQuiz;
    private Long startTime;
    private Long endTime;
    private String questionNumber;
    private boolean isLastQuestion;
    private Long initialTime;



    private Long closingTime;


    public Question() {

    }

    /*public Question(String question, String optionA, String optionB, String optionC, String optionD) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
    }*/

    public Question(String question, String optionA, String optionB, String optionC, String optionD, Integer count, Boolean isShowQuiz, Long startTime, Long endTime, String questionNumber, Boolean isLastQuestion, Long initialTime, Long closingTime) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.count = count;
        this.isShowQuiz = isShowQuiz;
        this.startTime = startTime;
        this.endTime = endTime;
        this.questionNumber = questionNumber;
        this.isLastQuestion = isLastQuestion;
        this.initialTime = initialTime;
        this.closingTime = closingTime;
    }

    public Long getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(Long closingTime) {
        this.closingTime = closingTime;
    }

    public Long getInitialTime() {
        return initialTime;
    }

    public void setInitialTime(Long initialTime) {
        this.initialTime = initialTime;
    }

    public String getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(String questionNumber) {
        this.questionNumber = questionNumber;
    }

    public boolean isLastQuestion() {
        return isLastQuestion;
    }

    public void setLastQuestion(boolean lastQuestion) {
        isLastQuestion = lastQuestion;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
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
