package model;

public class Address {
    private final String street;
    private final String city;
    private final String country;
    private final String zipCode;

    public Address(String street, String city, String country, String zipCode) {
        this.street  = street;
        this.city    = city;
        this.country = country;
        this.zipCode = zipCode;
    }

    public String getStreet()  { return street; }
    public String getCity()    { return city; }
    public String getCountry() { return country; }
    public String getZipCode() { return zipCode; }

    @Override
    public String toString() {
        return street + ", " + city + ", " + country + " - " + zipCode;
    }
}