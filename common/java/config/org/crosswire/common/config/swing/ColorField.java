
package org.crosswire.common.config.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.crosswire.common.config.Choice;
import org.crosswire.common.swing.GuiConvert;

/**
 * A Filename selection.
 * 
 * <p><table border='1' cellPadding='3' cellSpacing='0'>
 * <tr><td bgColor='white' class='TableRowColor'><font size='-7'>
 *
 * Distribution Licence:<br />
 * JSword is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License,
 * version 2 as published by the Free Software Foundation.<br />
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.<br />
 * The License is available on the internet
 * <a href='http://www.gnu.org/copyleft/gpl.html'>here</a>, or by writing to:
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA<br />
 * The copyright to this program is held by it's authors.
 * </font></td></tr></table>
 * @see gnu.gpl.Licence
 * @author Joe Walker [joe at eireneh dot com]
 * @version $Id$
 */
public class ColorField extends JPanel implements Field, ActionListener
{
    private static final String EDIT = "EditColor"; //$NON-NLS-1$

    /**
     * Create a new FileField
     */
    public ColorField()
    {
        initialize();
    }

    private void initialize()
    {
        actions = ButtonActionFactory.instance();
        actions.addActionListener(this);

        JButton edit = new JButton(actions.getAction(EDIT));
        edit.setIcon(new CustomIcon());
        edit.setMargin(new Insets(1, 2, 1, 1));

        setLayout(new BorderLayout());
        add(edit, BorderLayout.WEST);
        //add(text, BorderLayout.EAST);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        actions.actionPerformed(e, this);
    }

    /**
     * Do the edit action
     * @param ex
     */
    public void doEditColor(ActionEvent ex)
    {
        color = JColorChooser.showDialog(ColorField.this, Msg.EDIT.toString(), color);
    }

    /**
     * Some fields will need some extra info to display properly
     * like the options in an options field. FieldMap calls this
     * method with options provided by the choice.
     * @param param The options provided by the Choice
     */
    public void setChoice(Choice param)
    {
    }

    /**
     * Return a string version of the current value
     * @return The current value
     */
    public String getValue()
    {
        return GuiConvert.color2String(color);
    }

    /**
     * Set the current value
     * @param value The new text
     */
    public void setValue(String value)
    {
        color = GuiConvert.string2Color(value);
    }

    /**
     * Get the actual component that we can add to a Panel.
     * (This can well be this in an implementation).
     */
    public JComponent getComponent()
    {
        return this;
    }

    /**
     * The action factory for the buttons
     */
    private ButtonActionFactory actions;

    /**
     * The current Color
     */
    protected Color color = Color.white;

    /**
     * The icon square size
     */
    private static final int SIZE = 16;

    /**
     * The CustomIcon that shows the selected color
     */
    class CustomIcon implements Icon
    {
        /**
         * Returns the icon's height.
         */
        public int getIconHeight()
        {
            return SIZE;
        }

        /**
         * Returns the icon's width.
         */
        public int getIconWidth()
        {
            return SIZE;
        }

        /**
         * Draw the icon at the specified location.
         */
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            if (color == null)
            {
                g.setColor(Color.black);
                g.drawRect(x, y, SIZE, SIZE);
                g.drawLine(x, y, x+SIZE, y+SIZE);
                g.drawLine(x+SIZE, y, x, y+SIZE);
            }
            else
            {
                g.setColor(color);
                g.fillRect(x, y, SIZE, SIZE);
                g.setColor(Color.black);
                g.drawRect(x, y, SIZE, SIZE);
            }
        }
    }
}
