package com.dotcms.publisher.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class DependencyManager {
	
	private Set<String> hosts;
	private Set<String> folders;
	private Set<String> htmlPages;
	private Set<String> templates;
	private Set<String> structures;
	private Set<String> containers;
	private Set<String> contents;
	private Set<String> links;
	private User user;
	
	public DependencyManager(User user) { 
		hosts = new HashSet<String>();
		folders = new HashSet<String>();
		htmlPages = new HashSet<String>();
		templates = new HashSet<String>();
		structures = new HashSet<String>();
		containers = new HashSet<String>();
		contents = new HashSet<String>();
		links = new HashSet<String>();
		this.user = user;
	}
	
	public void setDependencies(PushPublisherConfig config) throws DotDataException {
		List<PublishQueueElement> assets = config.getAssets();
		
		for (PublishQueueElement asset : assets) {
			if(asset.getType().equals("htmlpage")) {
				htmlPages.add(asset.getAsset());
			} else if(asset.getType().equals("structure")) {
				structures.add(asset.getAsset());
			} else if(asset.getType().equals("template")) {
				templates.add(asset.getAsset());
			} else if(asset.getType().equals("containers")) {
				containers.add(asset.getAsset());
			} else if(asset.getType().equals("folder")) {
				folders.add(asset.getAsset());
			} else if(asset.getType().equals("host")) {
				hosts.add(asset.getAsset());
			}
		}
		
		setHostDependencies();
		setFolderDependencies();
		setHTMLPagesDependencies();
		setTemplateDependencies();
		setContainerDependencies();
		setStructureDependencies();
		
		config.setHostSet(hosts);
		config.setFolders(folders);
		config.setHTMLPages(htmlPages);
		config.setTemplates(templates);
		config.setContainers(containers);
		config.setStructures(structures);
		config.setContents(contents);
		config.setLinks(links);
	}
	
	private void setHostDependencies() {
		try {
			for (String id : hosts) {
				Host h = APILocator.getHostAPI().find(id, user, false);

				// Template dependencies
				List<Template> templateList = APILocator.getTemplateAPI().findTemplatesAssignedTo(h);
				for (Template template : templateList) {
					templates.add(template.getIdentifier());
				}
				
				// Container dependencies
				List<Container> containerList = APILocator.getContainerAPI().findContainersUnder(h);
				for (Container container : containerList) {
					containers.add(container.getIdentifier());
				}
				
				// Content dependencies
				String luceneQuery = "+conHost:" + h.getInode();
				
				List<Contentlet> contentList = APILocator.getContentletAPI().search(luceneQuery, 0, 0, null, user, false);
				for (Contentlet contentlet : contentList) {
					contents.add(contentlet.getIdentifier());
				}
				
				// Structure dependencies
				List<Structure> structuresList = StructureFactory.getStructuresUnderHost(h, user, false);;
				for (Structure structure : structuresList) {
					structures.add(structure.getInode());
				}
				
				// Folder dependencies
				List<Folder> folderList = APILocator.getFolderAPI().findFoldersByHost(h, user, false);
				for (Folder folder : folderList) {
					folders.add(folder.getInode());
				}
			}
			
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setFolderDependencies() {
		try {
			List<Folder> folderList = new ArrayList<Folder>();
			
			for (String id : folders) {
				Folder f = APILocator.getFolderAPI().find(id, user, false);
				// Parent folder
				Folder parent = APILocator.getFolderAPI().findParentFolder(f, user, false);
				if(UtilMethods.isSet(parent))
					folders.add(parent.getInode());
				
				folderList.add(f);
			}
			
			setFolderListDependencies(folderList);
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setFolderListDependencies(List<Folder> folderList) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		for (Folder f : folderList) {
			// Host dependency
			hosts.add(f.getHostId());
			
			// Content dependencies
			String luceneQuery = "+conFolder:" + f.getInode();
			
			List<Contentlet> contentList = APILocator.getContentletAPI().search(luceneQuery, 0, 0, null, user, false);
			for (Contentlet contentlet : contentList) {
				contents.add(contentlet.getIdentifier());
			}
			
			// Menu Link dependencies
			
			List<Link> linkList = APILocator.getMenuLinkAPI().findFolderMenuLinks(f);
			for (Link link : linkList) {
				links.add(link.getIdentifier());
			}
			
			// Structure dependencies
			List<Structure> structureList = APILocator.getFolderAPI().getStructures(f, user, false);
			
			for (Structure structure : structureList) {
				structures.add(structure.getInode());
			}
			
			// HTML Page dependencies
			List<HTMLPage> pages = APILocator.getFolderAPI().getHTMLPages(f, user, false);
			
			for (HTMLPage p : pages) {
				htmlPages.add(p.getIdentifier()); 
			}
			
			setFolderListDependencies(APILocator.getFolderAPI().findSubFolders(f, user, false));
		}
		
	}
	
	private void setHTMLPagesDependencies() {
		try {
			
			IdentifierAPI idenAPI = APILocator.getIdentifierAPI();
			FolderAPI folderAPI = APILocator.getFolderAPI();
			List<Container> containerList = new ArrayList<Container>();
			
			for (String pageId : htmlPages) {
				Identifier iden = idenAPI.find(pageId);
				
				// Host dependency
				hosts.add(iden.getHostId());
				Folder folder = folderAPI.findFolderByPath(iden.getParentPath(), iden.getHostId(), user, false);
				folders.add(folder.getInode());
				HTMLPage workingPage = APILocator.getHTMLPageAPI().loadWorkingPageById(pageId, user, false);
				HTMLPage livePage = APILocator.getHTMLPageAPI().loadLivePageById(pageId, user, false);
				
				// working template working page
				Template workingTemplateWP = APILocator.getTemplateAPI().findWorkingTemplate(workingPage.getTemplateId(), user, false);
				// live template working page
				Template liveTemplateWP = APILocator.getTemplateAPI().findLiveTemplate(workingPage.getTemplateId(), user, false);
				// live template live page
				Template liveTemplateLP = APILocator.getTemplateAPI().findLiveTemplate(livePage.getTemplateId(), user, false);
				
				// Templates dependencies
				templates.add(workingPage.getTemplateId());
				templates.add(livePage.getTemplateId());

				// Containers dependencies 
				containerList.clear();
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(workingTemplateWP, user, false));
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(liveTemplateWP, user, false));
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(liveTemplateLP, user, false));
				
				for (Container container : containerList) {
					containers.add(container.getIdentifier());
					// Structure dependencies
					structures.add(container.getStructureInode());
					List<MultiTree> treeList = MultiTreeFactory.getMultiTree(container.getIdentifier());
					
					for (MultiTree mt : treeList) {
						String contentIdentifier = mt.getChild();
						// Contents dependencies
						contents.add(contentIdentifier);
					}
				}
			}
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setTemplateDependencies() {
		try {
			List<Container> containerList = new ArrayList<Container>();
			
			for (String id : templates) {
				Template wkT = APILocator.getTemplateAPI().findWorkingTemplate(id, user, false);
				Template lvT = APILocator.getTemplateAPI().findLiveTemplate(id, user, false);
				
				// Host dependency
				hosts.add(APILocator.getTemplateAPI().getTemplateHost(wkT).getIdentifier()); 
				
				containerList.clear();
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(wkT, user, false));
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(lvT, user, false));
				
				for (Container container : containerList) {
					// Container dependencies
					containers.add(container.getIdentifier());
				}
			}
			
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	private void setContainerDependencies() {
		
		try {
		
			List<Container> containerList = new ArrayList<Container>();
			
			for (String id : containers) {
				Container c = APILocator.getContainerAPI().getWorkingContainerById(id, user, false);
			
				// Host Dependency
				hosts.add(APILocator.getContainerAPI().getParentHost(c, user, false).getIdentifier());
				
				containerList.clear();
				containerList.add(APILocator.getContainerAPI().getWorkingContainerById(id, user, false));
				containerList.add(APILocator.getContainerAPI().getLiveContainerById(id, user, false));
				
				for (Container container : containerList) {
					// Structure dependencies
					structures.add(container.getStructureInode());
				}
				
			}
		
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void setStructureDependencies() {
		
		for (String inode : structures) {
			Structure st = StructureCache.getStructureByInode(inode);
			hosts.add(st.getHost()); // add the host dependency
			folders.add(st.getFolder()); // add the folder dependency
		}
		
	}
	
	

}