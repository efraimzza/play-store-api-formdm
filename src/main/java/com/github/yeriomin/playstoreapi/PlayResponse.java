package com.github.yeriomin.playstoreapi;

public class PlayResponse {
    
    public boolean success=false;
    public int code=0;
    public String msg="no err";
    public byte[] response=null;
    public byte[] err=null;
    
    public PlayResponse(boolean msuccess, int mcode, String mmsg, byte[] mresponse, byte[] merr){
        this.success=msuccess;
        this.code=mcode;
        this.msg=mmsg;
        this.response=mresponse;
        this.err=merr;
    }
    
}
