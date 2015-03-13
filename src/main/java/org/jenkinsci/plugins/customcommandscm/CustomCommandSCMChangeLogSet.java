package org.jenkinsci.plugins.customcommandscm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.util.Digester2;
import hudson.util.IOException2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;



class CustomCommandSCMChangeLogSet extends ChangeLogSet<CustomCommandSCMChangeLogEntry> {
   private final List<CustomCommandSCMChangeLogEntry> changeLogEntries;
    
   public CustomCommandSCMChangeLogSet(AbstractBuild<?, ?> build, List<CustomCommandSCMChangeLogEntry> changeLogEntries) {
       super(build);
       this.changeLogEntries = changeLogEntries;
        for(CustomCommandSCMChangeLogEntry changeLogEntry: changeLogEntries) {
                changeLogEntry.setParent(this);
        }
   }
    
    @Override
    public boolean isEmptySet() {
        return this.changeLogEntries.isEmpty();
    }

    public Iterator<CustomCommandSCMChangeLogEntry> iterator() {
        return this.changeLogEntries.iterator();
    }
    
    public static class Parser extends ChangeLogParser {
        private static final Logger LOG = Logger.getLogger(Parser.class.getName());
                
        @Override
        public ChangeLogSet<? extends Entry> parse(AbstractBuild ab, File file) throws IOException, SAXException {
            List<CustomCommandSCMChangeLogEntry> changeLogEntries = new ArrayList<CustomCommandSCMChangeLogEntry>();

            Digester digester = new Digester2();
            digester.push(changeLogEntries);

            digester.addObjectCreate("*/entry", CustomCommandSCMChangeLogEntry.class);
            digester.addBeanPropertySetter("*/entry/date");
            digester.addBeanPropertySetter("*/entry/user-id", "userId");
            digester.addBeanPropertySetter("*/entry/changeset-number", "changesetNumber");
            digester.addBeanPropertySetter("*/entry/changeset-url", "changesetUrl");
            digester.addBeanPropertySetter("*/entry/comment", "comment");
            digester.addSetNext("*/entry", "add");

            digester.addObjectCreate("*/entry/items/item", CustomCommandSCMChangeLogEntry.Item.class);
            digester.addBeanPropertySetter("*/entry/items/item/change-type", "changeType");
            digester.addBeanPropertySetter("*/entry/items/item/file-name", "filename");
            digester.addSetNext("*/entry/items/item", "addFile");

            try {
                if(file.exists() && file.length() > 0) {
                    digester.parse(file);
                }
            }
            catch(IOException e) {
                    throw new IOException2("Failed to parse " + file, e);
            }
            catch(SAXException e) {
                    throw new IOException2("Failed to parse " + file, e);
            }

            return new CustomCommandSCMChangeLogSet(ab, changeLogEntries);
        }
        
    }
}