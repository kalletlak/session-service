package org.cbioportal.session_service.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(value = { "users", "created" }, allowGetters = true)
public class VirtualStudy {

	private Long created = System.currentTimeMillis();

	@NotNull
	private String name;

	@NotNull
	private String description;

	@NotNull
	private Set<Studies> studies;

	private AppliedFilters filters;

	@NotNull
	private String owner = "anonymous";

	@NotNull
	private Set<String> origin;

	private Set<String> users;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Studies> getStudies() {
		return studies;
	}

	public void setStudies(Set<Studies> studies) {
		this.studies = studies;
	}

	public AppliedFilters getFilters() {
		return filters;
	}

	public void setFilters(AppliedFilters filters) {
		this.filters = filters;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
		this.users = owner.equals("anonymous") ? new HashSet<String>() : Collections.singleton(owner);
	}

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}

	public Set<String> getOrigin() {
		return origin;
	}

	public void setOrigin(Set<String> origin) {
		this.origin = origin;
	}

	public Set<String> getUsers() {
		return users;
	}

	public void setUsers(Set<String> users) {
		this.users = users;
	}

}

class Studies {
	@NotNull
	private String id;
	private Set<String> samples;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<String> getSamples() {
		return samples;
	}

	public void setSamples(Set<String> samples) {
		this.samples = samples;
	}

}

class AppliedFilters {

	// TODO: current all filter values are considered as String
	private Map<String, Set<String>> patients;

	// TODO: current all filter values are considered as String
	private Map<String, Set<String>> samples;

	public Map<String, Set<String>> getPatients() {
		return patients;
	}

	public void setPatients(Map<String, Set<String>> patients) {
		this.patients = patients;
	}

	public Map<String, Set<String>> getSamples() {
		return samples;
	}

	public void setSamples(Map<String, Set<String>> samples) {
		this.samples = samples;
	}

}
