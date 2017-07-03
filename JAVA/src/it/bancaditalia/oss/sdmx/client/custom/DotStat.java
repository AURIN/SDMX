/* Copyright 2010,2014 Bank Of Italy
*
* Licensed under the EUPL, Version 1.1 or - as soon they
* will be approved by the European Commission - subsequent
* versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the
* Licence.
* You may obtain a copy of the Licence at:
*
*
* http://ec.europa.eu/idabc/eupl
*
* Unless required by applicable law or agreed to in
* writing, software distributed under the Licence is
* distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied.
* See the Licence for the specific language governing
* permissions and limitations under the Licence.
*/
package it.bancaditalia.oss.sdmx.client.custom;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import it.bancaditalia.oss.sdmx.api.DSDIdentifier;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.api.Dataflow;
import it.bancaditalia.oss.sdmx.client.SdmxClientHandler;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.exceptions.SdmxXmlContentException;
import it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser;
import it.bancaditalia.oss.sdmx.parser.v21.Sdmx21Queries;
import it.bancaditalia.oss.sdmx.util.Configuration;
import it.bancaditalia.oss.sdmx.util.RestQueryBuilder;

/**
 * @author Attilio Mattiocco
 *
 */
public abstract class DotStat extends RestSdmx20Client{
		
	protected static Logger logger = Configuration.getSdmxLogger();
	
	public DotStat(String name, URL endpoint, boolean needsCredentials, String format) throws MalformedURLException{
		super(name, endpoint, needsCredentials, null, format);
	}
	public DotStat(String name, URL endpoint, boolean needsCredentials) throws MalformedURLException{
		super(name, endpoint, needsCredentials, null, "compact_v2");
	}


	@Override
	public Dataflow getDataflow(String dataflow, String agency, String version) throws SdmxException {
		// OECD (and .Stat infrastructure) does not handle flows. We simulate it
		URL query = buildFlowQuery(dataflow, SdmxClientHandler.ALL_AGENCIES, SdmxClientHandler.LATEST_VERSION );
		List<DataFlowStructure> dsds = runQuery(new DataStructureParser(), query, null);
		if(dsds.size() > 0)
		{
			DataFlowStructure dsd = dsds.get(0);
			Dataflow result = new Dataflow();
			result.setAgency(dsd.getAgency());
			result.setId(dsd.getId());
			result.setVersion(dsd.getVersion());
			result.setName(dsd.getName());
			DSDIdentifier dsdId = new  DSDIdentifier();
			dsdId.setAgency(dsd.getAgency());
			dsdId.setId(dsd.getId());
			dsdId.setVersion(dsd.getVersion());
			result.setDsdIdentifier(dsdId);
			return result;
		}
		else
			throw new SdmxXmlContentException("The query returned zero dataflows");
	}

	@Override
	public Map<String, Dataflow> getDataflows() throws SdmxException {
		// OECD (and .Stat infrastructure) does not handle flows. We simulate it
		URL query = buildFlowQuery("ALL", SdmxClientHandler.ALL_AGENCIES, SdmxClientHandler.LATEST_VERSION );
		List<DataFlowStructure> dsds = runQuery(new DataStructureParser(), query, null);
		if(dsds.size() > 0)
		{
			Map<String, Dataflow> result = new HashMap<String, Dataflow>();
			for (Iterator<DataFlowStructure> iterator = dsds.iterator(); iterator.hasNext();) 
			{
				DataFlowStructure dsd = (DataFlowStructure) iterator.next();
				Dataflow df = new Dataflow();
				df.setAgency(dsd.getAgency());
				df.setId(dsd.getId());
				df.setVersion(dsd.getVersion());
				df.setName(dsd.getName());
				DSDIdentifier dsdId = new  DSDIdentifier();
				dsdId.setAgency(dsd.getAgency());
				dsdId.setId(dsd.getId());
				dsdId.setVersion(dsd.getVersion());
				df.setDsdIdentifier(dsdId);
				result.put(dsd.getId(), df);
			}
			
			return result;
		}
		else
			throw new SdmxXmlContentException("The query returned zero dataflows");
	}
	
	@Override
	protected URL buildFlowQuery(String flow, String agency, String version)  throws SdmxException{
		return(buildDSDQuery(flow, agency, version, false));
	}


	@Override
	protected URL buildDSDQuery(String dsd, String agency, String version, boolean full){
		if( endpoint!=null  && dsd!=null && !dsd.isEmpty()){
			return RestQueryBuilder.of(endpoint).path("GetDataStructure").path(dsd).build(needsURLEncoding);
		}
		else{
			throw new RuntimeException("Invalid query parameters: dsd=" + dsd + " endpoint=" + endpoint);
		}
	}

	@Override
	protected URL buildDataQuery(Dataflow dataflow, String resource, 
			String startTime, String endTime, 
			boolean serieskeysonly, String updatedAfter, boolean includeHistory) throws SdmxException{
		if( endpoint!=null && 
				dataflow!=null &&
				resource!=null && !resource.isEmpty()){
			
			// for OECD use the simple DF id
			RestQueryBuilder query = RestQueryBuilder.of(endpoint).path("GetData").path(dataflow.getId()).path(resource);
			
			//query=query+"?";
			//query += "&format=compact_v2";
			Sdmx21Queries.addParams(query, startTime, endTime, 
					serieskeysonly, updatedAfter, includeHistory, format);
			return query.build(needsURLEncoding);
		}
		else{
			throw new RuntimeException("Invalid query parameters: dataflow=" + dataflow + " resource=" + resource + " endpoint=" + endpoint);
		}
	}
}
