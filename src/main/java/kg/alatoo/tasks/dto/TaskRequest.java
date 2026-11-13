package kg.alatoo.tasks.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record TaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull @Positive Long projectId,
    @NotNull @Positive Long assigneeId,
    @NotNull @Positive Long statusId,
    LocalDate dueDate
) {}
