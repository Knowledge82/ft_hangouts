package com.fortytwo.ft_hangouts;

public class Contact {
    private long id;
    private String name;
    private String surname;
    private String phone;
    private String email;
    private String birthday;

    public Contact(String name, String surname, String phone, String email, String birthday) {
        this.name = name;
        this.surname = surname;
        this.phone = phone;
        this.email = email;
        this.birthday = birthday;
    }

    public Contact(long id, String name, String surname, String phone, String email, String birthday) {
        this(name, surname, phone, email, birthday);
        this.id = id;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getBirthday() { return birthday; }

    public void setName(String name) { this.name = name; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
}
