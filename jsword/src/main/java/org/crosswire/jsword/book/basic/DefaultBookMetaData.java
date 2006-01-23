/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 as published by
 * the Free Software Foundation. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * Copyright: 2005
 *     The copyright to this program is held by it's authors.
 *
 * ID: $Id$
 */
package org.crosswire.jsword.book.basic;

import java.util.Properties;

import org.crosswire.common.util.StringUtil;
import org.crosswire.common.xml.XMLUtil;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookCategory;
import org.crosswire.jsword.book.BookDriver;
import org.crosswire.jsword.book.BookMetaData;
import org.crosswire.jsword.book.OSISUtil;
import org.crosswire.jsword.index.IndexManager;
import org.crosswire.jsword.index.IndexManagerFactory;
import org.crosswire.jsword.index.IndexStatus;
import org.jdom.Document;
import org.jdom.Element;

/**
 * DefaultBookMetaData is an implementation of the of the BookMetaData
 * interface. A less complete implementation design for imheritance is
 * available in AbstractBookMetaData where the complexity is in the setup rather
 * than the inheritance. DefaultBookMetaData is probably the preferred
 * implementation.
 * 
 * @see gnu.lgpl.License for license details.
 *      The copyright to this program is held by it's authors.
 * @author Joe Walker [joe at eireneh dot com]
 */
public class DefaultBookMetaData extends AbstractBookMetaData
{
    /**
     * Ctor with a properties from which to get values.
     * A call to setBook() is still required after this ctor is called
     */
    public DefaultBookMetaData(BookDriver driver, Book book, Properties prop)
    {
        setDriver(driver);

        setProperties(prop);
        setName(prop.getProperty(BookMetaData.KEY_NAME));
        setType(prop.getProperty(BookMetaData.KEY_CATEGORY));
        setLanguage(prop.getProperty(BookMetaData.KEY_LANGUAGE));

        IndexManager imanager = IndexManagerFactory.getIndexManager();
        if (imanager.isIndexed(book))
        {
            setIndexStatus(IndexStatus.DONE);
        }
        else
        {
            setIndexStatus(IndexStatus.UNDONE);
        }
    }

    /**
     * Ctor with some default values.
     * A call to setBook() is still required after this ctor is called
     */
    public DefaultBookMetaData(BookDriver driver, String name, BookCategory type)
    {
        setDriver(driver);
        setName(name);
        setBookCategory(type);
        setLanguage(null); // Default language
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getType()
     */
    public BookCategory getBookCategory()
    {
        return type;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getName()
     */
    public String getName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getInitials()
     */
    public String getInitials()
    {
        return initials;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isLeftToRight()
     */
    public boolean isLeftToRight()
    {
        // TODO(joe): Do this correctly
        return true;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(String language)
    {
        putProperty(KEY_LANGUAGE, getLanguage(initials, language));
    }

    /**
     * See note on setName() for side effect on setInitials(). If a value of
     * null is used then the initials are defaulted using the name
     * @see DefaultBookMetaData#setName(String)
     * @param initials The initials to set.
     */
    public void setInitials(String initials)
    {
        if (initials == null)
        {
            if (name == null)
            {
                this.initials = ""; //$NON-NLS-1$
            }
            else
            {
                this.initials = StringUtil.getInitials(name);
            }
        }
        else
        {
            this.initials = initials;
        }

        putProperty(KEY_INITIALS, this.initials);
    }

    /**
     * Setting the name also sets some default initials, so if you wish to set
     * some specific initials then it should be done after setting the name.
     * @see DefaultBookMetaData#setInitials(String)
     * @param name The name to set.
     */
    public void setName(String name)
    {
        this.name = name;

        putProperty(KEY_NAME, this.name);

        setInitials(StringUtil.getInitials(name));
    }

    /**
     * @param aType The type to set.
     */
    public void setBookCategory(BookCategory aType)
    {
        BookCategory t = aType;
        if (t == null)
        {
            t = BookCategory.BIBLE;
        }
        type = t;

        putProperty(KEY_CATEGORY, type.toString());
    }

    /**
     * @param typestr The string version of the type to set.
     */
    public void setType(String typestr)
    {
        BookCategory newType = null;
        if (typestr != null)
        {
            newType = BookCategory.fromString(typestr);
        }

        setBookCategory(newType);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#toOSIS()
     */
    public Document toOSIS()
    {
        OSISUtil.ObjectFactory factory = OSISUtil.factory();
        Element ele = factory.createTable();
        addRow(ele, "Initials", getInitials()); //$NON-NLS-1$
        addRow(ele, "Description", getFullName()); //$NON-NLS-1$
        addRow(ele, "Key", getBookCategory().toString()); //$NON-NLS-1$
        addRow(ele, "Language", getLanguage()); //$NON-NLS-1$
        return new Document(ele);
    }

    private void addRow(Element table, String key, String value)
    {
        if (value == null)
        {
            return;
        }

        OSISUtil.ObjectFactory factory = OSISUtil.factory();

        Element rowEle = factory.createRow();

        Element nameEle = factory.createCell();
        Element hiEle = factory.createHI();
        hiEle.setAttribute(OSISUtil.ATTRIBUTE_HI_TYPE, OSISUtil.HI_BOLD);
        nameEle.addContent(hiEle);
        Element valueElement = factory.createCell();
        rowEle.addContent(nameEle);
        rowEle.addContent(valueElement);

        // I18N(DMS): use name to lookup translation.
        hiEle.addContent(key);

        String expandedValue = XMLUtil.escape(value);
        valueElement.addContent(expandedValue);

        table.addContent(rowEle);
    }

    private BookCategory type;
    private String name;
    private String initials;
}
