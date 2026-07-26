package com.example.LMS_BE.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {
  @NotBlank
  @Size(max = 50)
  private String studentCode;

  @NotBlank
  @Size(max = 150)
  private String fullName;

  @NotBlank
  @Email
  @Size(max = 150)
  private String email;

  @Size(max = 20)
  @Pattern(regexp = "^\\s*(?:(?:\\+?[1-9][0-9]{7,14}|0[0-9]{9,10}))?\\s*$")
  private String phone;

  private LocalDate dateOfBirth;

  @Size(max = 20)
  private String gender;

  @Size(max = 255)
  private String address;

  private List<@NotNull @Positive Long> retainedMediaIds;
  private List<@NotNull @Positive Long> newMediaIds;
  private List<@NotNull @Positive Long> removedMediaIds;

  public String studentCode() {
    return studentCode;
  }

  public String email() {
    return email;
  }

  public List<Long> retainedMediaIds() {
    return retainedMediaIds;
  }

  public List<Long> newMediaIds() {
    return newMediaIds;
  }

  public List<Long> removedMediaIds() {
    return removedMediaIds;
  }
}
