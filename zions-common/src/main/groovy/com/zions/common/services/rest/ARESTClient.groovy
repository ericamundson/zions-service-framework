package com.zions.common.services.rest

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.ResponseParseException
import groovyx.net.http.URIBuilder
//import org.codehaus.groovy.runtime.DefaultGroovyMethods;


import groovyx.net.http.RESTClient

class ARESTClient extends RESTClient {
    /**
     * Constructor.
     * @see HTTPBuilder#HTTPBuilder()
     */
    public ARESTClient() { super(); }

    /**
     * See {@link HTTPBuilder#HTTPBuilder(Object)}
     * @param defaultURI default request URI (String, URI, URL or {@link URIBuilder})
     * @throws URISyntaxException
     */
    public ARESTClient( Object defaultURI ) throws URISyntaxException {
        super( defaultURI );
    }

    /**
     * See {@link HTTPBuilder#HTTPBuilder(Object, Object)}
     * @param defaultURI default request URI (String, URI, URL or {@link URIBuilder})
     * @param defaultContentType default content-type (String or {@link ContentType})
     * @throws URISyntaxException
     */
    public ARESTClient( Object defaultURI, Object defaultContentType ) throws URISyntaxException {
        super( defaultURI, defaultContentType );
    }
	
	protected HttpResponseDecorator defaultSuccessHandler( HttpResponseDecorator resp, Object data )
			throws ResponseParseException {
		resp.setData(odefaultSuccessHandler( resp, data ) );
		return resp;
	}

	
    protected def odefaultSuccessHandler( HttpResponseDecorator resp, def parsedData )
            throws ResponseParseException {
        try {
            //If response is streaming, buffer it in a byte array:
            if ( parsedData instanceof InputStream ) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				buffer << parsedData;
				buffer.close()
                //DefaultGroovyMethods.leftShift( buffer, (InputStream)parsedData );
                parsedData = new ByteArrayInputStream( buffer.toByteArray() );
            }
            else if ( parsedData instanceof Reader ) {
                StringWriter buffer = new StringWriter();
				buffer << parsedData
				buffer.close()
                //DefaultGroovyMethods.leftShift( buffer, (Reader)parsedData );
                parsedData = new StringReader( buffer.toString() );
            }
            else if ( parsedData instanceof Closeable )
                log.warn( "Parsed data is streaming, but will be accessible after " +
                        "the network connection is closed.  Use at your own risk!" );
            return parsedData;
        }
        catch ( IOException ex ) {
            throw new ResponseParseException( resp, ex );
        }
    }
}
