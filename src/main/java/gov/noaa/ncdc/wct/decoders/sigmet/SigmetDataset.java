package gov.noaa.ncdc.wct.decoders.sigmet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.MAMath;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.RadialDatasetSweep.RadialVariable;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.LatLonRect;


//public class SigmetDataset extends RadialDatasetSweepAdapter implements TypedDatasetFactoryIF {
public class SigmetDataset implements RadialDatasetSweep, FeatureDataset {
	
    static private NetcdfDataset ds;
    double latv, lonv, elev;
    int nsweeps;
    ucar.unidata.geoloc.EarthLocation origin;
    DateFormatter formatter = new DateFormatter();
    Date startDate;
    Date endDate;
    DateUnit dateUnits;
    
    // constructors
    public SigmetDataset() {}
    public SigmetDataset(NetcdfDataset ds) {
        this.ds = ds;

        setEarthLocation();
        setTimeUnits();
        setStartDate();
        setEndDate();
//        setBoundingBox();
    }

    protected void setEarthLocation() {
        latv = 0.0; 
        lonv = 0.0;
        elev = 0.0;  
        Attribute ga = ds.findGlobalAttribute("radar_lat");
        if(ga != null ) latv = ga.getNumericValue().doubleValue();

        ga = ds.findGlobalAttribute("radar_lon");
        if(ga != null) lonv = ga.getNumericValue().doubleValue();

        ga = ds.findGlobalAttribute("ground_height");
        if(ga != null) elev = ga.getNumericValue().doubleValue();
        origin = new EarthLocationImpl(latv, lonv, elev);
    }

    // you must set EarthLocation before you call this.
//    protected void setBoundingBox() {
//      LatLonRect largestBB = null;
//      // look through all the coord systems
//      for (Object o : csHash.values()) {
//        RadialCoordSys sys = (RadialCoordSys) o;
//        sys.setOrigin(origin);
//        LatLonRect bb = sys.getBoundingBox();
//        if (largestBB == null)
//          largestBB = bb;
//        else
//          largestBB.extend(bb);
//      }
//      boundingBox = largestBB;
//    }
    
    public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
        return origin;
    } 
    public String getRadarID() { 
        return (ds.findGlobalAttribute("StationName_SetupUtility")).getStringValue();  
    }
    public String getRadarName() { 
        return (ds.findGlobalAttribute("StationName")).getStringValue(); 
    }

    public String getDataFormat() {
        return null;
    }

    public boolean isVolume() {
        return true;
    }
    public boolean isStationary() {
        return true;
    }

    protected void setTimeUnits() {
        List axes = ds.getCoordinateAxes();
        for (int i = 0; i < axes.size(); i++) {
            CoordinateAxis axis = (CoordinateAxis) axes.get(i);
            if (axis.getAxisType() == AxisType.Time) {
                String units = axis.getUnitsString();       
                try {
                    dateUnits = new DateUnit(units);        
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }
    protected void setStartDate() {
        String start_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_start", null);
        try {
            if (start_datetime != null) {
                startDate = formatter.getISODate(start_datetime);  
            } else {  System.out.println("** start_datetime is NULL!!!"); }
        } catch (Exception e) { System.out.println(e.toString()); }
    }

    protected void setEndDate() {
        String end_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_end", null);
        if (end_datetime != null)
            endDate = formatter.getISODate(end_datetime);
        else
            System.out.println("** end_datetime is NULL!!!");  
    }
    public void clearDatasetMemory() {
        List  rvars = getDataVariables();
        Iterator iter = rvars.iterator();
        while (iter.hasNext()) {
            RadialVariable radVar = (RadialVariable)iter.next();
            radVar.clearVariableMemory();
        }
    }

    protected void addRadialVariable(NetcdfDataset nds, Variable var) {
        RadialVariable rsvar = null;
        int rnk = var.getRank();
        VariableSimpleIF v = new SigmetVariable(nds, var);
        rsvar = makeRadialVariable(nds, var);          
//        if(rsvar != null) dataVariables.add(rsvar);      
    }
    protected RadialVariable makeRadialVariable(NetcdfDataset nds, Variable v0) {
//        return new SigmetVariable(nds, v, v0);
        return new SigmetVariable(nds, v0);
    }

    @Override
    public java.util.List<VariableSimpleIF> getDataVariables() {
    	List<Variable> dataVariables = ds.getVariables();
        java.util.List<VariableSimpleIF> dVar=new java.util.ArrayList<VariableSimpleIF> ();
        int nparams=ds.findGlobalAttribute("num_data_types").getNumericValue().intValue(); 
        nsweeps = ds.findGlobalAttribute("number_sweeps").getNumericValue().intValue(); 
        for (int i=0; i< nparams*nsweeps; i++) { 
            Variable v =dataVariables.get(i); 
            if (v.getShortName().startsWith("Total_Power") ||  
                v.getShortName().startsWith("Reflectivity") ||
                v.getShortName().startsWith("Velocity") ||
                v.getShortName().startsWith("Width")) {
            	
            	dVar.add(new SigmetVariable(ds, v));
            }
        }      
        return dVar;
    }
    public String getInfo() {
        StringBuffer sbuff = new StringBuffer();
        sbuff.append("SigmetDataset\n");
        return sbuff.toString();
    }

    
    
    
    
    public class SigmetVariable implements RadialDatasetSweep.RadialVariable {
        int nrays, ngates, nsweeps, number_sweeps; 
        int[] number_gates;
        ArrayList sweeps;
        String name;
        Variable var;

        public SigmetVariable(NetcdfDataset nds, Variable v0) {
        	this.var = v0;
        	
            sweeps=new ArrayList();  
            name=v0.getShortName();    //System.out.println("------- VAR NAME="+name+" -------------");
            int[] shape = v0.getShape();   //System.out.println("---SHAPE="+shape.length);
            int count = v0.getRank()-1;    //System.out.println("---RANK="+v0.getRank()+"  COUNT="+count);
            if (v0.getRank()>1) {
                ngates = shape[count];      //System.out.println("---TOTAL:  NGATES="+ngates);
                count--;                       //System.out.println("---COUNT="+count);
                nrays = shape[count];      // System.out.println("---TOTAL: NRAYS="+nrays);
                nsweeps=1;
                sweeps.add(new SigmetSweep(v0, 0, nrays, ngates));
            } else {  if (name.startsWith("distanceR")) { 
                nrays=0; ngates=shape[0];  nsweeps=1; //System.out.println("---DIST:  NGATES="+ngates);
                sweeps.add(new SigmetSweep(v0, 0, nrays, ngates));          
            } else if (name.startsWith("time") || name.startsWith("azimuth") || name.startsWith("elevation")) { 
                nrays=shape[0]; ngates=0; nsweeps=1;
                sweeps.add(new SigmetSweep(v0, 0, nrays, ngates));          
            } else { nsweeps=shape[0];   //System.out.println("---NUM_GATES: NSWEEPS="+nsweeps);
            sweeps.add(new SigmetSweep(v0, nsweeps, 0, 0));         
            }
            }   
            try {
                number_gates=(int[])nds.findVariable("numGates").read().get1DJavaArray(Integer.TYPE);  
                number_sweeps=number_gates.length;   
            } catch (IOException e) { System.out.println(e.toString()); }
        }

        public void setNumberSweeps() {   //get number sweeps per data file 
            number_sweeps = ds.findGlobalAttribute("number_sweeps").getNumericValue().intValue();     
        } 
        public int getNumberSweeps() {
            setNumberSweeps();   
            return number_sweeps;
        }  


        public String toString() {
            return name;
        }

        public int getNumSweeps() {  
            return nsweeps;
        }

        public SigmetSweep getSweep(int sweepNo) {
            SigmetSweep sw = null;     
            return (SigmetSweep)sweeps.get(0);   
        }

        public int getNumRadials() {
            return nsweeps * nrays;
        }

        public float[] readAllData() throws IOException {
            Array allData=null;   // System.out.println("HERE!!!");
            SigmetSweep spn=(SigmetSweep)sweeps.get(sweeps.size()-1);
            Variable v=spn.getsweepVar();  //System.out.println("InREAD: VAR="+v.getName())  
            try {            
                allData = v.read();    //System.out.println("InREAD: "+allData.toString());   
            } catch (IOException e) { throw new IOException(e.getMessage()); }
            return (float []) allData.get1DJavaArray(float.class);
        }
        public void clearVariableMemory() {   } 

        public class SigmetSweep implements ucar.nc2.dt.RadialDatasetSweep.Sweep{
            double meanElevation = Double.NaN;
            double meanAzimuth = Double.NaN;
            int sweepno, nrays, ngates;
            Variable sweepVar;
            float radDist;  
            String nameVar;

            public SigmetSweep(Variable v, int sweepno, int rays, int gates) {
                nameVar=v.getName();        // System.out.println("+++ NAME="+nameVar);      
                this.sweepVar=v;
                this.nrays=rays;
                this.ngates=gates;    //System.out.println("+++ NGATES="+ngates);
                this.sweepno = sweepno;    //System.out.println("+++ SWEEPNO="+sweepno);
            }

            public Variable getsweepVar() {
                return sweepVar;
            }

            // read 2d sweep data nradials * ngates 
            public float[] readData() throws java.io.IOException {
                Array sweepTmp=null;
                try {
                    sweepTmp = sweepVar.read().reduce();
                } catch (java.io.IOException e ) {   e.printStackTrace();  }
                return (float []) sweepTmp.get1DJavaArray(Float.TYPE);         
            }
            // read the radial data   
            public float[] readData(int ray) throws java.io.IOException {
                Array sweepTmp = null;     
                int[] shape = sweepVar.getShape();     
                int[] origin = new int[2];   
                origin[0] = ray; 
                origin[1] = 0;     
                shape[0] = 1;
                shape[1] = ngates;
                try {
                    sweepTmp = sweepVar.read(origin, shape).reduce();    
                } catch (Exception e) {  e.printStackTrace();  }
                return (float []) sweepTmp.get1DJavaArray(Float.TYPE);
            }

            public int getGateNumber() {
                int gates=number_gates[0];   
                if (number_sweeps > 1) { 
                    String[] s=nameVar.split("_");
                    int ix=Integer.parseInt(s[s.length-1]);
                    gates=number_gates[ix-1];
                }       
                //System.out.println("GATES in sweep="+gates);  
                return gates;                
            }

            public int getRadialNumber() {
                return nrays;
            }

            public RadialDatasetSweep.Type getType() {
                return null;
            }

            public ucar.unidata.geoloc.EarthLocation getOrigin(int ray) {
                return origin;
            }

            public String getStartSweep() {
                String s="start_sweep";   
                if (number_sweeps > 1) { 
                    String[] sp=nameVar.split("_");
                    sweepno=Integer.parseInt(sp[sp.length-1]);       
                    s="start_sweep_"+sweepno; 
                }
                Attribute ga = ds.findGlobalAttribute(s);
                return ga.getStringValue();      
            } 

            public Date getStartingTime() {
                return startDate;
            }

            public Date getEndingTime() {
                return endDate;
            }

            public int getSweepIndex() {
                return sweepno;
            }

            public boolean isConic() {
                return true;
            }

            public float getElevation(int ray) throws IOException {
                Array eleData = null;
                Index index =null;
                try {
                    String eleName="elevationR";  
                    if (number_sweeps >1 ) {
                        String[] s=nameVar.split("_");
                        sweepno=Integer.parseInt(s[s.length-1]);   
                        eleName= "elevationR_sweep_"+(sweepno);     
                    }          
                    eleData = ds.findVariable(eleName).read();
                    index = eleData.getIndex();                  
                } catch (IOException e) { e.printStackTrace(); }     
                return eleData.getFloat(index.set(ray));
            }  

            public float[] getElevation() throws IOException {
                Array eleData = null;
                try {
                    String eleName="elevationR";     
                    if (number_sweeps >1 ) {
                        String[] s=nameVar.split("_");
                        sweepno=Integer.parseInt(s[s.length-1]);      
                        eleName= "elevationR_sweep_"+(sweepno); 
                    }    
                    eleData = ds.findVariable(eleName).read();                
                } catch (IOException e) {  e.printStackTrace(); }       
                return (float [])eleData.get1DJavaArray(Float.TYPE);
            }

            public void setMeanElevation() {    
                Array eleData = null;
                float sum = 0.f;
                int sumSize = 0;
                try {
                    String eleName="elevationR";     
                    if (number_sweeps >1 ) {
                        String[] s=nameVar.split("_");
                        sweepno=Integer.parseInt(s[s.length-1]);        
                        eleName= "elevationR_sweep_"+(sweepno); 
                    }   
                    eleData =  ds.findVariable(eleName).read();               
                    //NCdump.printArray(eleData, "elevationR", System.out, null);           
                } catch (IOException e) { e.printStackTrace();  }
                meanElevation=MAMath.sumDouble(eleData) / eleData.getSize();  
            }

            public float getMeanElevation() {
                if( Double.isNaN(meanElevation))  setMeanElevation();
                return (float) meanElevation;
            } 

            public double meanDouble(Array a) {
                double sum = 0;
                int size = 0;
                IndexIterator iterA = a.getIndexIterator();
                while (iterA.hasNext()) {
                    double s = iterA.getDoubleNext();
                    if (! Double.isNaN(s)) {
                        sum += s;
                        size ++;
                    }
                }
                return sum / size;
            }

            public float[] getAzimuth() throws IOException {
                Array aziData = null;
                try {
                    String aziName="azimuthR";  
                    if (number_sweeps >1 ) {
                        String[] s=nameVar.split("_");
                        sweepno=Integer.parseInt(s[s.length-1]);       
                        aziName= "azimuthR_sweep_"+(sweepno); 
                    }       
                    aziData = ds.findVariable(aziName).read();               
                } catch (IOException e) { e.printStackTrace(); }
                return (float [])aziData.get1DJavaArray(Float.TYPE);
            }

            public float getAzimuth(int ray) throws IOException {
                Array aziData = null;
                try {
                    String aziName="azimuthR";     
                    if (number_sweeps >1 ) {
                        String[] s=nameVar.split("_");
                        sweepno=Integer.parseInt(s[s.length-1]);         
                        aziName= "azimuthR_sweep_"+(sweepno);
                    }     
                    aziData = ds.findVariable(aziName).read();               
                } catch (IOException e) {  e.printStackTrace(); }
                Index index = aziData.getIndex();
                return aziData.getFloat(index.set(ray));
            }

            public void setMeanAzimuth() {
                Array aziData = null;
                meanAzimuth = 0.0;
                try {
                    String aziName="azimuthR";     
                    if (number_sweeps >1 ) {
                        String[] s=nameVar.split("_");
                        sweepno=Integer.parseInt(s[s.length-1]);        
                        aziName= "azimuthR_sweep_"+(sweepno);
                    }      
                    aziData =  ds.findVariable(aziName).read();               
                    meanAzimuth = MAMath.sumDouble( aziData) / aziData.getSize();
                } catch (IOException e) { e.printStackTrace();  meanAzimuth = 0.0;}
            }

            public float getMeanAzimuth() {
                if(Double.isNaN(meanAzimuth)) setMeanAzimuth();
                return (float) meanAzimuth;
            }

            public float getTime(int ray) throws IOException {      
                String tName="time";     
                if (number_sweeps >1 ) { 
                    String[] s=nameVar.split("_");
                    sweepno=Integer.parseInt(s[s.length-1]);      
                    tName= "time_sweep_"+(sweepno); 
                }  
                //System.out.println("TIME : sweepno="+sweepno+" tName="+tName);   
                return  getT(tName, sweepno, ray);
            }

            public float getT(String tName, int swpNumber, int ray) throws IOException {
                Array timeData = ds.findVariable(tName).read(); 
                Index timeIndex = timeData.getIndex();
                return timeData.getFloat(timeIndex.set(ray));
            }

            public float getBeamWidth() {
                return 0.95f; // degrees, info from Chris Burkhart
            }
            public float getNyquistFrequency() {
                return 0; // LOOK this may be radial specific
            }
            public float getRangeToFirstGate() {
                return 0.0f;
            }

            public float getRadialDistance(int gate) {
                Array radDist=null;      
                String dd="distanceR";
                if (number_sweeps > 1) { 
                    String[] s=nameVar.split("_");
                    sweepno=Integer.parseInt(s[s.length-1]);      
                    dd="distanceR_sweep_"+(sweepno); 
                }
                try {  radDist=ds.findVariable(dd).read();                      
                } catch (IOException e) { e.printStackTrace(); return 0.0f; }  
                Index distIndex = radDist.getIndex();
                return radDist.getFloat(distIndex.set(gate)); 
            }

            public float getGateSize() {
                float r1=0.0f, r2=0.0f;      
                Attribute ga1 = ds.findGlobalAttribute("range_first");
                if(ga1 != null)  r1= ga1.getNumericValue().floatValue();   
                Attribute ga2 = ds.findGlobalAttribute("range_last");
                if(ga2 != null)  r2= ga2.getNumericValue().floatValue();  
                return (r2-r1)/(getGateNumber()-1);
            }

            public boolean isGateSizeConstant() {
                return true;
            }

            public void clearSweepMemory() {
            }
        } // ----------SigmetSweep class

		@Override
		public Attribute findAttributeIgnoreCase(String attName) {
		      Iterator it = var.getAttributes().iterator();
		        Attribute at = null;
		        while(it.hasNext()){
		           at = (Attribute)it.next();
		           if(attName.equalsIgnoreCase(at.getShortName()))
		              break;
		        }
		        return at;
		}

		@Override
		public List<Attribute> getAttributes() {
			return var.getAttributes();
		}

		@Override
		public DataType getDataType() {
			return DataType.FLOAT;
		}

		@Override
		public String getDescription() {
			return "Radial Variable: "+var.getFullName();
		}

		@Override
		public List<Dimension> getDimensions() {
			return null;
		}

		@Override
		public String getFullName() {
			return var.getFullName();
		}

		@Override
		public String getName() {
			return var.getShortName();
		}

		@Override
		public int getRank() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int[] getShape() {
			return var.getShape();
		}

		@Override
		public String getShortName() {
			return var.getShortName();
		}

		@Override
		public String getUnitsString() {
			return var.getUnitsString();
		}

		@Override
		public int compareTo(VariableSimpleIF v) {
			return getFullName().compareTo(v.getFullName());
		}
    }
    // SigmetVariable class------------------------------------------
    /*
  private static void testRadialVariable(RadialDatasetSweep.RadialVariable rv) throws IOException {
    int nsweep = rv.getNumSweeps();
    System.out.println("*** radar Sweep number is: \n" + nsweep);
    Sweep sw;
    float mele;
    for (int i = 0; i < nsweep; i++) {
      //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable getSweep " + i);
      sw = rv.getSweep(i);
      //mele = sw.getMeanElevation();
      //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable getSweep " + i);
      float me = sw.getMeanElevation();

      System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
      int nrays = sw.getRadialNumber();
      float [] az = new float[nrays];
      for (int j = 0; j < nrays; j++) {
        float azi = sw.getAzimuth(j);
        az[j] = azi;
      }
      float [] azz = sw.getAzimuth();
      float [] dat = sw.readData();
      // System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
    }
    sw = rv.getSweep(0);
      //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable readData");
    float [] data = rv.readAllData();
    float [] ddd = sw.readData();
    float [] da = sw.getAzimuth();
    float [] de = sw.getElevation();
      //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable readData");
    int nrays = sw.getRadialNumber();
    float [] az = new float[nrays];
    for (int i = 0; i < nrays; i++) {
      int ngates = sw.getGateNumber();
      float [] d = sw.readData(i);
     // float [] e = sw.readDataNew(i);
     // assert(null != e);
      float azi = sw.getAzimuth(i);
      az[i] = azi;
      float ele = sw.getElevation(i);
      float la = (float) sw.getOrigin(i).getLatitude();
      float lo = (float) sw.getOrigin(i).getLongitude();
      float al = (float) sw.getOrigin(i).getAltitude();
    }
  }

  public static void main(String args[]) throws Exception, IOException, InstantiationException, IllegalAccessException {
    String fileIn = "C:/netcdf/data/sig4.dat";
    //String fileIn ="/upc/share/testdata/radar/NOP3_20071112_1633";
   // RadialDatasetSweepFactory datasetFactory = new RadialDatasetSweepFactory();
   // RadialDatasetSweep rds = datasetFactory.open(fileIn, null);
 // ucar.unidata.util.Trace.call1("LevelII2Dataset:main dataset");
    RadialDatasetSweep rds = (RadialDatasetSweep) TypedDatasetFactory.open( 
     ucar.nc2.constants.FeatureType.RADIAL, fileIn, null, new StringBuilder());
 // ucar.unidata.util.Trace.call2("LevelII2Dataset:main dataset"); 
    String st = rds.getStartDate().toString();
    String et = rds.getEndDate().toString();
    String id = rds.getRadarID();
    String name = rds.getRadarName();
    if (rds.isStationary()) {
      System.out.println("*** radar is stationary with name and id: " + name + " " + id);
    }
    List rvars = rds.getDataVariables();
    RadialDatasetSweep.RadialVariable vDM = (RadialDatasetSweep.RadialVariable) rds.getDataVariable("Total_Power_24");
    testRadialVariable(vDM);
    for (int i = 0; i < rvars.size(); i++) {
     // RadialDatasetSweep.RadialVariable rv = (RadialDatasetSweep.RadialVariable) rvars.get(i);
     //  testRadialVariable(rv);

      //  RadialCoordSys.makeRadialCoordSys( "desc", CoordinateSystem cs, VariableEnhanced v);
      // ucar.nc2.dt.radial.RadialCoordSys rcsys = rv.getRadialCoordSys();
    }

  }
     */





	@Override
	public void close() throws IOException {
		ds.close();
	}
	@Override
	public Attribute findGlobalAttributeIgnoreCase(String name) {
		return ds.findGlobalAttributeIgnoreCase(name);
	}
	@Override
	public LatLonRect getBoundingBox() {
		return null;
	}
	@Override
	public VariableSimpleIF getDataVariable(String varName) {
		for (VariableSimpleIF var : getDataVariables()) {
			if (var.getShortName().equals(varName)) {
				return var;
			}
		}
		return null;
	}
	@Override
	public String getDescription() {
		return "SIGMET Dataset";
	}
	@Override
	public String getDetailInfo() {
		return "SIGMET Dataset detailed info goes here";
	}
	@Override
	public Date getEndDate() {
		return endDate;
	}
	@Override
	public List<Attribute> getGlobalAttributes() {
		return ds.getGlobalAttributes();
	}
	@Override
	public String getLocationURI() {
		return ds.getReferencedFile().getLocation();
	}
	@Override
	public NetcdfFile getNetcdfFile() {
		return ds.getReferencedFile();
	}
	@Override
	public Date getStartDate() {
		return startDate;
	}
	@Override
	public String getTitle() {
		return ds.getTitle();
	}
	@Override
	public long getLastModified() {
		return ds.getLastModified();
	}
	@Override
	public void calcBounds() throws IOException {
		
		
	}
	@Override
	public CalendarDate getCalendarDateEnd() {
		return CalendarDate.of(endDate);
	}
	@Override
	public CalendarDateRange getCalendarDateRange() {
		return CalendarDateRange.of(getStartDate(), getEndDate());
	}
	@Override
	public CalendarDate getCalendarDateStart() {
		return CalendarDate.of(startDate);
	}
	@Override
	public DateRange getDateRange() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void getDetailInfo(Formatter fmt) {
		fmt.format(getDescription());		
	}
	@Override
	public FeatureType getFeatureType() {
		return FeatureType.RADIAL;
	}
	@Override
	public String getImplementationName() {
		return getClass().getName();
	}
	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Type getCommonType() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public DateUnit getTimeUnits() {
		return dateUnits;
	}
	@Override
	public void reacquire() throws IOException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void release() throws IOException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setFileCache(FileCacheIF cache) {
		ds.setFileCache(cache);		
	}
    
    
}