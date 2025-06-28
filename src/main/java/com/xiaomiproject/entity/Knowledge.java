package com.xiaomiproject.entity;

import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "konwledges")
@Data
public class Knowledge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;
}
