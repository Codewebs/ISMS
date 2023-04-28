package com.indiza.smsi.data;

public class Message {
    private int IDmessage;
    private String sender;
    private String receiver;
    private String contenu;
    private boolean statut;

    public Message(int IDmessage, String sender, String receiver, String contenu, boolean statut) {
        this.IDmessage = IDmessage;
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.statut = statut;
    }

    public int getIDmessage() {
        return IDmessage;
    }

    public void setIDmessage(int IDmessage) {
        this.IDmessage = IDmessage;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public boolean isStatut() {
        return statut;
    }

    public void setStatut(boolean statut) {
        this.statut = statut;
    }
}
