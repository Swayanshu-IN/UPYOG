package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import org.hibernate.validator.constraints.SafeHtml;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Transaction object representing a transaction
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Transaction {

    
    @JsonProperty("tenantId")
    private String tenantId;
    

    /**
     * Backward compatibility
     */
    @JsonProperty("module")
    @Size(min = 1)
    private String module;

    /**
     * Backward compatibility
     */
    @SafeHtml
    @JsonProperty("consumerCode")
    private String consumerCode;

    /**
     * Generated by the app, after transaction is initiated
     */
    @JsonProperty("txnId")
    private String txnId;
    	
	@JsonProperty("pdfUrl")
	private String pdfUrl;
	
	@JsonProperty("fileStoreId")
	private String fileStoreId;
	
	@JsonProperty("redirectUrl")
	private String redirectUrl;
	
	@JsonProperty("signedFilestoreId")
	private String signedFilestoreId;

    @JsonProperty("createdBy")
    private String createdBy = null;

    @JsonProperty("createdTime")
    private Long createdTime = null;

    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy = null;

    @JsonProperty("lastModifiedTime")
    private Long lastModifiedTime = null;
	
}