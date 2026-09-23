import express from "express";
import cors from "cors";

const app=express();
app.use(cors());
app.use(express.json());

const PORT=process.env.PORT||3000;
const API_KEY=process.env.GPS_API_KEY||"change-this-key";
const vehicles=new Map();

function auth(req,res,next){
  const key=req.get("x-gps-api-key");
  if(key!==API_KEY)return res.status(401).json({error:"Unauthorized"});
  next();
}

app.get("/health",(req,res)=>res.json({ok:true,service:"Vehicle Tracker GPS API"}));

app.post("/telemetry",auth,(req,res)=>{
  const {deviceId,vehicleNo,lat,lng,accuracy}=req.body;
  const latitude=Number(lat),longitude=Number(lng);
  if(!deviceId||!vehicleNo||!Number.isFinite(latitude)||!Number.isFinite(longitude)||
     latitude<-90||latitude>90||longitude<-180||longitude>180){
    return res.status(400).json({error:"deviceId, vehicleNo, valid lat and lng are required"});
  }
  const plate=String(vehicleNo).trim().toUpperCase().replace(/\s+/g,"");
  const data={deviceId:String(deviceId),vehicleNo:plate,lat:latitude,lng:longitude,accuracy:accuracy??"GPS",updatedAt:new Date().toISOString()};
  vehicles.set(plate,data);
  res.json({ok:true,data});
});

app.get("/vehicle/:vehicleNo",auth,(req,res)=>{
  const plate=req.params.vehicleNo.trim().toUpperCase().replace(/\s+/g,"");
  const data=vehicles.get(plate);
  if(!data)return res.status(404).json({error:"No GPS telemetry received for this vehicle"});
  res.json({lat:data.lat,lng:data.lng,accuracy:data.accuracy,updatedAt:data.updatedAt});
});

app.listen(PORT,()=>console.log("GPS API running on port "+PORT));