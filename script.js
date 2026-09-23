const demoVehicles={HR26AB1234:{name:"Demo Car",lat:28.6139,lng:77.209,accuracy:"±8 m"},DL01XY5678:{name:"Demo SUV",lat:28.5355,lng:77.391,accuracy:"±12 m"}};
const savedKey="vehicleTrackerAuthorizedVehicles";
let customVehicles=JSON.parse(localStorage.getItem(savedKey)||"{}");
let map=null,marker=null;

const $=id=>document.getElementById(id);
function allVehicles(){return {...demoVehicles,...customVehicles}}
function normalize(v){return v.trim().toUpperCase().replace(/\s+/g,"")}
function save(){localStorage.setItem(savedKey,JSON.stringify(customVehicles));renderVehicleList()}
function renderVehicleList(){
 const list=$("vehicleList");list.innerHTML="";
 Object.entries(allVehicles()).forEach(([plate,v])=>{
  const chip=document.createElement("div");chip.className="vehicle-chip";
  const text=document.createElement("span");text.textContent=plate+" • "+v.name;
  chip.appendChild(text);
  if(customVehicles[plate]){const del=document.createElement("button");del.textContent="✕";del.title="Remove local vehicle";del.onclick=()=>{delete customVehicles[plate];save()};chip.appendChild(del)}
  list.appendChild(chip)
 })
}
function track(){
 const plate=normalize($("vehicleNo").value);$("message").textContent="";
 if(!plate){$("message").textContent="Please enter a vehicle number.";return}
 const v=allVehicles()[plate];
 if(!v){$("message").textContent="Vehicle is not registered in this authorized demo account."; $("result").classList.add("hidden");return}
 $("plate").textContent=plate;$("vehicleName").textContent=v.name;
 $("lat").textContent=v.lat.toFixed(6)+"°";$("lng").textContent=v.lng.toFixed(6)+"°";
 $("accuracy").textContent=v.accuracy;$("updated").textContent=new Date().toLocaleString();
 $("result").classList.remove("hidden");
 setTimeout(()=>showMap(v,plate),50);
}
function showMap(v,plate){
 if(!map){map=L.map("map").setView([v.lat,v.lng],14);L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",{maxZoom:19,attribution:"© OpenStreetMap contributors"}).addTo(map)}
 else map.setView([v.lat,v.lng],14);
 if(marker)marker.remove();
 marker=L.marker([v.lat,v.lng]).addTo(map).bindPopup("<b>"+plate+"</b><br>"+v.name+"<br>Demo GPS position").openPopup();
 map.invalidateSize();
}
$("trackBtn").addEventListener("click",track);$("vehicleNo").addEventListener("keydown",e=>{if(e.key==="Enter")track()});
$("addBtn").addEventListener("click",()=>{
 const plate=normalize($("regNo").value),name=$("regName").value.trim()||"My Vehicle",lat=Number($("regLat").value),lng=Number($("regLng").value);
 if(!plate||!Number.isFinite(lat)||!Number.isFinite(lng)||lat<-90||lat>90||lng<-180||lng>180){$("regMessage").textContent="Enter a valid vehicle number, latitude and longitude.";return}
 customVehicles[plate]={name,lat,lng,accuracy:"Demo accuracy"};save();$("regMessage").textContent=plate+" added to this browser's authorized demo list."; $("vehicleNo").value=plate;
 $("regNo").value="";$("regName").value="";$("regLat").value="";$("regLng").value="";
});
renderVehicleList();