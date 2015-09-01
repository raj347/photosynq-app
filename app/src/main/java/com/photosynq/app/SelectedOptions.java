package com.photosynq.app;

/**
 * Created by shekhar on 8/25/15.
 */
public class SelectedOptions {


    private String questionId;
    private String projectId;
    private int questionType;
    private boolean remember;
    private String rangeFrom;
    private String rangeTo;
    private String rangeRepeat;
    private int currentRangeValue;
    private int optionType;
    private String selectedValue;
    private String urlValue;
    private boolean reset;

    public int getAutoIncIndex() {
        return autoIncIndex;
    }

    public void setAutoIncIndex(int autoIncIndex) {
        this.autoIncIndex = autoIncIndex;
    }

    private int autoIncIndex;



    public int getOptionType() {
        return optionType;
    }

    public void setOptionType(int optionType) {
        this.optionType = optionType;
    }

    public boolean isReset() {
        return reset;
    }

    public void setReset(boolean reset) {
        this.reset = reset;
    }

    public String getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    public String getUrlValue() {
        return urlValue;
    }

    public void setUrlValue(String urlValue) {
        this.urlValue = urlValue;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public int getQuestionType() {
        return questionType;
    }

    public void setQuestionType(int questionType) {
        this.questionType = questionType;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public boolean isRemember() {
        return remember;
    }

    public void setRemember(boolean remember) {
        this.remember = remember;
    }

    public int getCurrentRangeValue() {
        return currentRangeValue;
    }

    public void setCurrentRangeValue(int currentRangeValue) {
        this.currentRangeValue = currentRangeValue;
    }

    public String getRangeFrom() {
        return rangeFrom;
    }

    public void setRangeFrom(String rangeFrom) {
        this.rangeFrom = rangeFrom;
    }

    public String getRangeTo() {
        return rangeTo;
    }

    public void setRangeTo(String rangeTo) {
        this.rangeTo = rangeTo;
    }

    public String getRangeRepeat() {
        return rangeRepeat;
    }

    public void setRangeRepeat(String rangeRepeat) {
        this.rangeRepeat = rangeRepeat;
    }
}
