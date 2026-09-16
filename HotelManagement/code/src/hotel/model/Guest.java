package hotel.model;

public class Guest {
    // email is the unique identity
    private final String email;
    private String name;
    private String phone;

    public Guest(String email, String name, String phone) {
        this.email = email;
        this.name  = name;
        this.phone = phone;
    }

    public String getId()    { return email; }
    public String getEmail() { return email; }
    public String getName()  { return name; }
    public String getPhone() { return phone; }

    public void setName(String name)   { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return "Guest{email='" + email + "', name='" + name + "', phone='" + phone + "'}";
    }
}
