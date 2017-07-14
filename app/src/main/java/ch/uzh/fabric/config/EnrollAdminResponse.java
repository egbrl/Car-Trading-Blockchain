package ch.uzh.fabric.config;

public class EnrollAdminResponse extends JsonObject {
    private String name;
    private String organization;
    private String mspId;
    private boolean enrolled;
    private boolean registered;

  public EnrollAdminResponse(String name, String organization, String mspId, boolean enrolled, boolean registered) {
    this.name = name;
    this.organization = organization;
    this.mspId = mspId;
    this.enrolled = enrolled;
    this.registered = registered;
  }
  
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * @param organization the organization to set
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * @return the mspId
   */
  public String getMspId() {
    return mspId;
  }

  /**
   * @param mspId the mspId to set
   */
  public void setMspId(String mspId) {
    this.mspId = mspId;
  }

  /**
   * @return the enrolled
   */
  public boolean isEnrolled() {
    return enrolled;
  }

  /**
   * @param enrolled the enrolled to set
   */
  public void setEnrolled(boolean enrolled) {
    this.enrolled = enrolled;
  }

  /**
   * @return the registered
   */
  public boolean isRegistered() {
    return registered;
  }

  /**
   * @param registered the registered to set
   */
  public void setRegistered(boolean registered) {
    this.registered = registered;
  }
}
