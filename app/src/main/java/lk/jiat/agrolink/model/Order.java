package lk.jiat.agrolink.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Order implements Serializable {
    private int id;
    private double totalPrice;
    private String status;
    private Date orderDate;
    private User user;
    private List<OrderItem> orderItems;
    
    // ✅ ඇණවුම බෙදා හැරිය යුතු ස්ථානය (Latitude/Longitude)
    private Double latitude;
    private Double longitude;

    public Order() {}

    public Order(User user, List<OrderItem> orderItems, double totalPrice, String status) {
        this.user = user;
        this.orderItems = orderItems;
        this.totalPrice = totalPrice;
        this.status = status;
        this.orderDate = new Date();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    // ✅ Getters and Setters for Location
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
