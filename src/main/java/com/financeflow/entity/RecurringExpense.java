package com.financeflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_expenses")
public class RecurringExpense {
    public enum Frequency { WEEKLY, MONTHLY, YEARLY }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, columnDefinition = "ENUM('Food','Transportation','Education','Shopping','Entertainment','Bills','Health','Salary','Other')")
    private String category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('WEEKLY','MONTHLY','YEARLY')")
    private Frequency frequency;
    @Column(name = "next_payment_date", nullable = false)
    private LocalDate nextPaymentDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
