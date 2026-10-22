package model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Contact (US5: contacts with a name, an address and a mobile
 * number).
 * All three are text fields (plan 4.2).
 */
class ContactTest {

    private Contact newContact() {
        return new Contact(2, "Chan Tai Man", "Hung Hom", "91234567");
    }

    // Checks: a contact reports its type as "contact".
    @Test
    void getType_isContact() {
        assertEquals("contact", newContact().getType());
    }

    // Checks: the constructor stores the ID, name, address and mobile.
    @Test
    void constructor_storesAllFields() {
        Contact contact = newContact();

        assertEquals(2, contact.getId());
        assertEquals(Map.of("name", "Chan Tai Man", "address", "Hung Hom", "mobile", "91234567"),
                contact.get_text_fields());
    }

    // Checks: a contact has no time fields.
    @Test
    void getTimeFields_isEmpty() {
        assertTrue(newContact().get_time_fields().isEmpty());
    }

    // Checks: the declared field names are "name", "address", "mobile" (plan 4.2),
    // no time fields.
    @Test
    void fieldNames_matchSpecification() {
        Contact contact = newContact();

        assertEquals(Set.of("name", "address", "mobile"), contact.getTextFields());
        assertEquals(Set.of(), contact.getTimeFields());
    }

    // Checks (US6): set_field() on "name" replaces the name.
    @Test
    void setField_name_updatesName() {
        Contact contact = newContact();

        contact.set_field("name", "Wong Siu Ming");

        assertEquals("Wong Siu Ming", contact.get_text_fields().get("name"));
    }

    // Checks (US6): set_field() on "address" replaces the address.
    @Test
    void setField_address_updatesAddress() {
        Contact contact = newContact();

        contact.set_field("address", "Tsim Sha Tsui");

        assertEquals("Tsim Sha Tsui", contact.get_text_fields().get("address"));
    }

    // Checks (US6): set_field() on "mobile" replaces the mobile number.
    @Test
    void setField_mobile_updatesMobile() {
        Contact contact = newContact();

        contact.set_field("mobile", "98765432");

        assertEquals("98765432", contact.get_text_fields().get("mobile"));
    }

    // Checks: set_field() rejects field names that contacts do not have,
    // with InvalidFieldException.
    @Test
    void setField_unknownField_throwsInvalidFieldException() {
        Contact contact = newContact();

        assertThrows(InvalidFieldException.class, () -> contact.set_field("email", "a@b.com"));
        assertThrows(InvalidFieldException.class, () -> contact.set_field("deadline", "anything"));
    }

    // Checks: the mobile number is stored as text, so a leading "+" and spaces are
    // kept.
    @Test
    void mobile_isStoredAsTextUnchanged() {
        Contact contact = new Contact(2, "Chan Tai Man", "Hung Hom", "+852 9123 4567");

        assertEquals("+852 9123 4567", contact.get_text_fields().get("mobile"));
    }

    // Checks (US8): the detail string shows the ID, name, address and mobile.
    @Test
    void toDetailString_containsAllFields() {
        String details = newContact().to_detail_string();

        assertTrue(details.contains("2"));
        assertTrue(details.contains("Chan Tai Man"));
        assertTrue(details.contains("Hung Hom"));
        assertTrue(details.contains("91234567"));
    }

    // Checks (US10): to_record() gives the ID, type and every field, ready to be
    // saved.
    @Test
    void toRecord_containsIdTypeAndFields() {
        Map<String, String> expected = Map.of(
                "id", "2", "type", "contact", "name", "Chan Tai Man", "address", "Hung Hom", "mobile", "91234567");

        assertEquals(expected, newContact().to_record());
    }
}
