proxy
=====
connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(request);
            connection.setRequestProperty("Content-Type", 
                        "application/x-www-form-urlencoded");
           // connection.setRequestProperty("Content-Length","");
            connection.setRequestProperty("Content-Language", "en-US");
            //connection.setRequestProperty("Connection", "closed");
                
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
                
            //Send request
            DataOutputStream wr = new DataOutputStream (
                         connection.getOutputStream ());
            // wr.writeBytes(""); //urlParamters eventually
            wr.flush();
            wr.close();
            // read the response from the server. 
            //Get Response  
            InputStream is;
            if (connection.getResponseCode() >= 400) {
                is = connection.getErrorStream();
            } else {
                is = connection.getInputStream();
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer(); 
            while((line = rd.readLine()) != null) {
              //response.append(line);
              //response.append('\r');
              ostream.write(line.toString().getBytes(Charset.forName("UTF-8")));
            }
            rd.close();
            //System.out.println(response.toString());
      
            //byte buffer[] = new byte[1024];
            
            //ostream.write(response.toString().getBytes(Charset.forName("UTF-8")));