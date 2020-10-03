package com.example.reader.Notification;

public class Data {
    private  String tittle, body;

    public Data() {
    }
    public Data(String tittle, String body) {
        this.tittle = tittle;
        this.body = body;
    }


    public String getTitle() {
        return tittle;
    }

    public void setTitle(String title) {
        this.tittle = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
