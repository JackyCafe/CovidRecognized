package com.ian.covidrecognized;

public class FileSturct {
    private String type;
    private String file_name;
    public FileSturct(String type,String file_name){
        this.type = type;
        this.file_name = file_name;
    }

    public String getType() {
        return type;
    }

    public String getFile_name() {
        return file_name;
    }

    @Override
    public String toString() {
        return "FileSturct{" +
                "type='" + type + '\'' +
                ", file_name='" + file_name + '\'' +
                '}';
    }
}
