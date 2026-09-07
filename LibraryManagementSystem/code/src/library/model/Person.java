package library.model;

/**
 * Abstract base for any person in the system.
 * id is set to email — email is the stable unique identity.
 * Callers must never change email after construction.
 */
public abstract class Person {

    protected final String id;   // == email
    protected String name;
    protected String email;
    protected String phone;

    protected Person(String name, String email, String phone) {
        this.id    = email;      // email is the unique key
        this.name  = name;
        this.email = email;
        this.phone = phone;
    }

    public String getId()    { return id; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void setName(String name)   { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    // no setEmail — email is identity; changing it would break repository keying
}
