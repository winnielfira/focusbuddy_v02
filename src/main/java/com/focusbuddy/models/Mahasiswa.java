package com.focusbuddy.models;

public class Mahasiswa extends User {
    private String studentId;
    private String major;
    private String bio;
    private String profileImagePath;
    
    public Mahasiswa() {
        super();
    }
    
    public Mahasiswa(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    
    @Override
    public String getUserType() {
        return "MAHASISWA";
    }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
}
