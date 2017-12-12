/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal Session Service.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.session_service.service.internal;

import org.cbioportal.session_service.service.SessionService;
import org.cbioportal.session_service.service.exception.*;
import org.cbioportal.session_service.util.OperationType;
import org.cbioportal.session_service.domain.Session;
import org.cbioportal.session_service.domain.SessionRepository;
import org.cbioportal.session_service.domain.VirtualStudy;

import com.mongodb.util.JSONParseException;
import java.lang.IllegalArgumentException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;

import javax.validation.ConstraintViolationException;
import javax.validation.ConstraintViolation;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author Manda Wilson 
 */
@Service
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Override
    public Session addSession(String source, String type, String data) throws SessionInvalidException {
        Session session = null; 
        try {
            session = new Session(source, type, data);
            sessionRepository.saveSession(session);
        } catch (DuplicateKeyException e) {
            session = sessionRepository.findOneBySourceAndTypeAndData(source,
                type,
                session.getData());
        } catch (ConstraintViolationException e) {
            throw new SessionInvalidException(buildConstraintViolationExceptionMessage(e));
        } catch (JSONParseException e) {
            throw new SessionInvalidException(e.getMessage());
        }
        return session;
    }

    @Override
    public List<Session> getSessions(String source, String type) {
        return sessionRepository.findBySourceAndType(source, type);
    }

    @Override
    public List<Session> getSessionsByQuery(String source, String type, String field, String value) 
        throws SessionQueryInvalidException {
        try {
            return sessionRepository.findBySourceAndTypeAndQuery(source, type, field, value);
        } catch (IllegalArgumentException e) {
            throw new SessionQueryInvalidException(e.getMessage());
        } catch (UncategorizedMongoDbException e) {
            throw new SessionQueryInvalidException(e.getMessage());
        }
    }

    @Override
    public Session getSession(String source, String type, String id) throws SessionNotFoundException {
        Session session = sessionRepository.findOneBySourceAndTypeAndId(source, type, id);
        if (session != null) {
            return session;
        }
        throw new SessionNotFoundException(id);
    }

    @Override
    public void updateSession(String source, String type, String id, String data, Optional<OperationType> operation) throws SessionInvalidException, 
        SessionNotFoundException {
        Session savedSession = sessionRepository.findOneBySourceAndTypeAndId(source, type, id);
        if (savedSession != null) {
            try {
	            	if(savedSession.getData() instanceof VirtualStudy && type.equals("virtual_study") && operation.isPresent()) {
	            		savedSession.updateUserInVirtualStudy(data, operation.get());
	            	}else {
	            		savedSession.setData(data);
	            	}
                sessionRepository.saveSession(savedSession);
            } catch (ConstraintViolationException e) {
                throw new SessionInvalidException(buildConstraintViolationExceptionMessage(e));
            } catch (JSONParseException e) {
                throw new SessionInvalidException(e.getMessage());
            }   
            return;
        }
        throw new SessionNotFoundException(id);
    }

    @Override
    public void deleteSession(String source, String type, String id) throws SessionNotFoundException {
        int numberDeleted = sessionRepository.deleteBySourceAndTypeAndId(source, type, id);
        if (numberDeleted != 1) { // using unique id so never more than 1 
            throw new SessionNotFoundException(id);
        }
    }

    private String buildConstraintViolationExceptionMessage(ConstraintViolationException e) {
        StringBuffer errors = new StringBuffer();
        for (ConstraintViolation violation : e.getConstraintViolations()) {
            errors.append(violation.getMessage());
            errors.append(";");
        }
        return errors.toString();
    }

}
