package fu.de190530.post.dto;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import fu.de190530.post.common.Create;

public class PostDTO {
    private Long postId;

    @NotBlank(groups = Create.class) @Size(max = 100)
    private String advertiserName;

    @NotBlank(groups = Create.class) @Email @Size(max = 100)
    private String advertiserEmail;

    @NotBlank(groups = Create.class) @Size(max = 20)
    private String advertiserPhone;

    @NotNull(groups = Create.class)
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC", lenient = OptBoolean.FALSE)
    private Date startDate;

    @NotNull(groups = Create.class)
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC", lenient = OptBoolean.FALSE)
    private Date endDate;

    @NotNull(groups = Create.class) @Min(1)
    private Integer numberOfPosts;

    @DecimalMin("0") @Digits(integer = 16, fraction = 2)
    private BigDecimal totalCost;

    @Pattern(regexp = "DRAFT|SCHEDULED|PUBLISHED|CANCELLED")
    private String status;

    @NotNull(groups = Create.class) @Min(1)
    private Long userId;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getAdvertiserName() { return advertiserName; }
    public void setAdvertiserName(String advertiserName) { this.advertiserName = advertiserName; }

    public String getAdvertiserEmail() { return advertiserEmail; }
    public void setAdvertiserEmail(String advertiserEmail) { this.advertiserEmail = advertiserEmail; }

    public String getAdvertiserPhone() { return advertiserPhone; }
    public void setAdvertiserPhone(String advertiserPhone) { this.advertiserPhone = advertiserPhone; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Integer getNumberOfPosts() { return numberOfPosts; }
    public void setNumberOfPosts(Integer numberOfPosts) { this.numberOfPosts = numberOfPosts; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

}
