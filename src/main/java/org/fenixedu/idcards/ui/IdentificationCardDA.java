/**
 * Copyright © 2014 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Identification Cards.
 *
 * FenixEdu Identification Cards is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Identification Cards is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Identification Cards.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.idcards.ui;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.fenixedu.domain.Person;
import net.sourceforge.fenixedu.injectionCode.AccessControl;
import net.sourceforge.fenixedu.presentationTier.Action.base.FenixDispatchAction;
import net.sourceforge.fenixedu.presentationTier.Action.person.PersonApplication.PersonalAreaApp;
import net.sourceforge.fenixedu.util.BundleUtil;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.bennu.portal.EntryPoint;
import org.fenixedu.bennu.portal.StrutsFunctionality;
import org.fenixedu.idcards.domain.SantanderCardInformation;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.joda.time.DateTime;

import pt.ist.fenixWebFramework.struts.annotations.Forward;
import pt.ist.fenixWebFramework.struts.annotations.Forwards;
import pt.ist.fenixWebFramework.struts.annotations.Mapping;

@StrutsFunctionality(app = PersonalAreaApp.class, descriptionKey = "label.identification.card", path = "identification-card",
        titleKey = "label.identification.card")
@Mapping(module = "person", path = "/identificationCard")
@Forwards(@Forward(name = "show.card.information", path = "/person/identificationCard/showCardInformation.jsp"))
public class IdentificationCardDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepare(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request,
            final HttpServletResponse response) throws Exception {
        final Person person = AccessControl.getPerson();
        final String cardProdutionState = getCardProdutionState(person);

        request.setAttribute("person", person);
        request.setAttribute("state", cardProdutionState);
        return mapping.findForward("show.card.information");
    }

    private String getCardProdutionState(Person person) {
        /*verify first step - information sent to SIBS*/
        Set<SantanderEntry> entries = person.getSantanderEntriesSet();
        SantanderEntry lastEntry = null;
        for (SantanderEntry entry : entries) {
            if (entry.getSantanderBatch().getSent() == null) {
                continue;
            }
            if (lastEntry == null || entry.getSantanderBatch().getSent().isAfter(lastEntry.getSantanderBatch().getSent())) {
                lastEntry = entry;
            }
        }

        Set<SantanderCardInformation> cards = person.getSantanderCardsInformationSet();
        if (lastEntry == null) {
            return BundleUtil.getStringFromResourceBundle("resources.ApplicationResources",
                    "label.identification.card.production.state.one");
        }
        if (cards.isEmpty()) {
            return BundleUtil.getStringFromResourceBundle("resources.ApplicationResources",
                    "label.identification.card.production.state.two", new String[] { lastEntry.getSantanderBatch().getSent()
                            .toString("dd/MM/yyyy") });
        }
        /*verify second step - SIBS emitted the card*/
        SantanderCardInformation lastCard = null;
        for (SantanderCardInformation card : cards) {
            DateTime prod_card_date = SantanderCardInformation.getProductionDateTime(card.getDchpRegisteLine());
            if (lastCard == null
                    || prod_card_date.isAfter(SantanderCardInformation.getProductionDateTime(lastCard.getDchpRegisteLine()))) {
                lastCard = card;
            }
        }
        DateTime prod_card_date = SantanderCardInformation.getProductionDateTime(lastCard.getDchpRegisteLine());
        return BundleUtil.getStringFromResourceBundle("resources.ApplicationResources",
                "label.identification.card.production.state.three", new String[] { prod_card_date.toString("dd/MM/yyyy") });
    }

}
