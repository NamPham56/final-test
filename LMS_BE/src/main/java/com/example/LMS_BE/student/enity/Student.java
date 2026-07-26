package com.example.LMS_BE.student.enity;

import com.example.LMS_BE.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "students")
public class Student extends BaseEntity {

  @Column(name = "student_code", nullable = false, length = 50)
  private String studentCode;

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(name = "email", nullable = false, length = 150)
  private String email;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(name = "gender", length = 20)
  private String gender;

  @Column(name = "address", length = 255)
  private String address;
}
