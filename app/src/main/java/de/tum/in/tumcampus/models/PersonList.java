package de.tum.in.tumcampus.models;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Wrapper class holding a list of persons. Note: This model is based on the
 * TUMOnline web service response format for a corresponding request.
 */

@SuppressWarnings("UnusedDeclaration")
@Root(name = "rowset")
public class PersonList {

	@ElementList(inline = true)
	private List<Person> persons;

	public List<Person> getPersons() {
		return persons;
	}

	public void setPersons(List<Person> persons) {
		this.persons = persons;
	}

}
