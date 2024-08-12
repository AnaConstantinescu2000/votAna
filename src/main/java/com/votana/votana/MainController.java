package com.votana.votana;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.regex.Pattern;

import org.springframework.web.bind.annotation.*;

@RestController
public class MainController {
    Connection conn = null;

    public MainController() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost/votrec?user=root&password=1234&zeroDateTimeBehavior=convertToNull");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean esteCNPValid(String cnp) {
        if (cnp == null || cnp.length() != 13 || !Pattern.matches("\\d{13}", cnp)) {
            return false;
        }

        int[] constant = {2, 7, 9, 1, 4, 6, 3, 5, 8, 2, 7, 9};
        int sum = 0;

        for (int i = 0; i < 12; i++) {
            sum += (cnp.charAt(i) - '0') * constant[i];
        }

        int control = sum % 11;
        if (control == 10) {
            control = 1;
        }

        return control == (cnp.charAt(12) - '0');
    }

    @RequestMapping(value="/incarcaJudete/{cnp}")
    public String incarcaJudete(@PathVariable String cnp) throws SQLException
    {

        //Logica functiei
        conn = DriverManager.getConnection("jdbc:mysql://localhost/votrec?user=root&password=1234&zeroDateTimeBehavior=convertToNull");
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery("SELECT judete.judet, localitati.id AS id_localitate, localitati.localitate FROM votrec.judete LEFT JOIN votrec.localitati ON judete.id = localitati.id_judet ORDER BY judete.judet, localitati.localitate");

        String resJudet = "";
        String tmpJudet = "";
        while (rs.next())
        {
            String judet = rs.getString("judet");
            String id_localitate = rs.getString("id_localitate");
            String localitate = rs.getString("localitate");

            if(!tmpJudet.contentEquals(judet)) {
                resJudet += "<button type='button' class='btn btn-secondary mb-2 mt-2 d-block'>" + judet + "</button>";
            }
            tmpJudet = judet;

            resJudet += "<a href='candidati.html?cnp=" + cnp + "&id_localitate=" + id_localitate + "' class='btn btn-info ms-2 me-2' role='button'>" + localitate + "</a>";
        }

        return resJudet;
    }


    @RequestMapping(value="/incarcaCandidati/{cnp}/{id_localitate}")
    public String incarcaCandidati(@PathVariable String cnp, @PathVariable String id_localitate) throws SQLException
    {

        conn = DriverManager.getConnection("jdbc:mysql://localhost/votrec?user=root&password=1234&zeroDateTimeBehavior=convertToNull");
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery("SELECT candidati.id AS id_candidatidat, candidati.nume AS numeCandidat, candidati.partid AS partid_candidatidat, candidati.poza AS pozaCandidat, localitati.localitate FROM votrec.candidati LEFT JOIN votrec.localitati ON localitati.id = candidati.id_localitate WHERE localitati.id =" + id_localitate);

        String resCandidati = "";
        String tmpLocalitate = "";
        while (rs.next())
        {
            String localitate = rs.getString("localitate");
            String id_candidatidat = rs.getString("id_candidatidat");
            String numeCandidat = rs.getString("numeCandidat");
            String partid_candidatidat = rs.getString("partid_candidatidat");
            String pozaCandidat = rs.getString("pozaCandidat");

            if(!tmpLocalitate.contentEquals(localitate)) {
                resCandidati += "<button type='button' class='btn btn-secondary mb-2 mt-2 d-block'>" + localitate + "</button>";
            }
            tmpLocalitate = localitate;

            resCandidati += "<a href='vot.html?cnp=" + cnp + "&id_candidat=" + id_candidatidat + "' class='ms-2 me-2 text-decoration-none d-grid'>" + numeCandidat + " (" + partid_candidatidat + ")";
            resCandidati += "<img class='mx-auto mt-2 img' src='/images/candidati/" + pozaCandidat + "'height='180'>";
            resCandidati += "</a>";
        }

        return resCandidati;
    }

    @RequestMapping(value="/adaugaVotant/{cnp}", method=RequestMethod.POST)
    public String adaugaVotant(@PathVariable String cnp) throws SQLException {

        conn = DriverManager.getConnection("jdbc:mysql://localhost/votrec?user=root&password=1234&zeroDateTimeBehavior=convertToNull");
        Statement statement = conn.createStatement();

        try {
            String countSQL = "SELECT COUNT(*) FROM votanti";
            Statement countStatement = conn.createStatement();
            ResultSet rs = countStatement.executeQuery(countSQL);
            int nextId = 1;
            if (rs.next()) {
                nextId = rs.getInt(1) + 1;
            }

            String insertVotantSQL = "INSERT INTO votanti (id, cnp) VALUES (?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(insertVotantSQL);
            preparedStatement.setInt(1, nextId);
            preparedStatement.setString(2, cnp);
            preparedStatement.executeUpdate();

            return "Votantul a fost adăugat cu ID-ul: " + nextId;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Eroare la adăugarea votantului: " + e.getMessage();
        }
    }

    @RequestMapping(value="/adaugaVot/{idVotant}/{idCandidat}", method=RequestMethod.POST)
    public String adaugaVot(@PathVariable String idVotant, @PathVariable String idCandidat) throws SQLException {

        conn = DriverManager.getConnection("jdbc:mysql://localhost/votrec?user=root&password=1234&zeroDateTimeBehavior=convertToNull");
        Statement statement = conn.createStatement();


        try {
            String countSQL = "SELECT COUNT(*) FROM voturi";
            Statement countStatement = conn.createStatement();
            ResultSet rs = countStatement.executeQuery(countSQL);
            int nextId = 1;
            if (rs.next()) {
                nextId = rs.getInt(1) + 1;
            }

            String insertVotSQL = "INSERT INTO voturi (id, id_votant, id_candidat) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(insertVotSQL);
            preparedStatement.setInt(1, nextId);
            preparedStatement.setInt(2, Integer.parseInt(idVotant));
            preparedStatement.setInt(3, Integer.parseInt(idCandidat));
            preparedStatement.executeUpdate();

            return "Votul a fost înregistrat cu succes!";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Eroare la înregistrarea votului: " + e.getMessage();
        }
    }

    @RequestMapping(value="/voteazaCandidat/{cnp}/{idCandidat}", method=RequestMethod.GET)
    public String voteazaCandidat(@PathVariable String cnp, @PathVariable String idCandidat) {
        try {
            String adaugaVotantResult = adaugaVotant(cnp);
            if (adaugaVotantResult.startsWith("Eroare") || adaugaVotantResult.equals("CNP invalid.")) {
                return adaugaVotantResult;
            }

            String verificaVotSQL = "SELECT id FROM votanti WHERE cnp = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(verificaVotSQL);
            preparedStatement.setString(1, cnp);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                int idVotant = rs.getInt("id");
                return adaugaVot(String.valueOf(idVotant), idCandidat);
            } else {
                return "CNP-ul nu este valid!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Eroare la votare: " + e.getMessage();
        }
    }

    @RequestMapping(value="/verificaVot/{cnp}")
    public String verificaVot(@PathVariable String cnp) throws SQLException
    {
        String resVot = "";
        if (!esteCNPValid(cnp)) {
            return "CNP invalid.";
        }

        ResultSet vot = existaVot(cnp);
        if(vot != null) {
            if (vot.next()) {
                resVot = "Stimate utilizator <B>" + vot.getString("numeVotant") + "</B> (adresa: " + vot.getString("adresaVotant") + "), ați votat deja cu <B>" + vot.getString("numeCandidat") + "</B> (" + vot.getString("partid_candidatidat") + ")";
                resVot += "<img class='d-block mx-auto mt-2 img' src='/images/candidati/" + vot.getString("pozaCandidat") + "'height='180'>";
            }
        }

        if(resVot.equals(""))
        {
            resVot = "Nu ați votat! Apăsați pe butonul de mai jos pentru a vota!<br><br>";
            resVot += "<a href='judete.html?cnp=" + cnp + "' class='btn btn-info' role='button'>Votați acum</a>";
        }
        return resVot;
    }

    public ResultSet existaVot(String recnp) throws SQLException
    {
        ResultSet resVotant;

        if((recnp == null) || (recnp.isEmpty()) || (recnp.trim().isEmpty()))
        {
            resVotant = null;
        }
        else
        {
            conn = DriverManager.getConnection("jdbc:mysql://localhost/votrec?user=root&password=1234&zeroDateTimeBehavior=convertToNull");
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT votanti.nume AS numeVotant, votanti.cnp AS cnpVotant, votanti.id_localitate AS id_localitateVotant, votanti.adresa AS adresaVotant, candidati.nume AS numeCandidat, candidati.partid AS partid_candidatidat, candidati.poza AS pozaCandidat FROM votanti LEFT JOIN voturi ON votanti.id = voturi.id_votant LEFT JOIN candidati ON candidati.id = voturi.id_candidat WHERE votanti.cnp = '" + recnp + "'");
            resVotant = rs;
        }

        return resVotant;
    }

    @RequestMapping("/getLocalitateByJudet")
    public String getLocalitateByJudet(@RequestParam String codJudet){
        System.out.println(codJudet);
        return codJudet;
    }

}
