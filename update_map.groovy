@Grapes([
    @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.3'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-distribution', version='5.1.14'),
    @GrabConfig(systemClassLoader=true)
])

import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.parameters.*
import org.semanticweb.elk.owlapi.*
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.io.*
import org.semanticweb.owlapi.owllink.*
import org.semanticweb.owlapi.util.*
import org.semanticweb.owlapi.search.*
import org.semanticweb.owlapi.manchestersyntax.renderer.*
import org.semanticweb.owlapi.reasoner.structural.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import groovyx.gpars.*
import org.codehaus.gpars.*

def map = [:]
new File('map.tsv').splitEachLine('\t') {
  if(it[0] == 'ukb id') { return; }
  map[it[0]] = [
    'ukb id': it[0],
    'ukb label': it[1],
    'ecmo iri': it[2],
    'ecmo label': it[3]
  ]
}

def manager = OWLManager.createOWLOntologyManager()
def fac = manager.getOWLDataFactory()
def ecmo = manager.loadOntologyFromOntologyDocument(new File("ecmo.owl"))

ecmo.getClassesInSignature(true).each { cl ->
  def label
  def ukb
  EntitySearcher.getAnnotations(cl, ecmo).each { anno ->
    def property = anno.getProperty()
    OWLAnnotationValue val = anno.getValue()
    if(val instanceof OWLLiteral) {
      def literal = val.getLiteral()
      if(property.isLabel()) {
        label = literal
      } else if (literal =~ 'UKB') {
        ukb = literal.tokenize(':')[1]
      }
    }
  }

  def iri = cl.getIRI().toString()
  //println iri
  if(ukb) {
    if(!map[ukb]) {
      println 'adding new map'
      map[ukb] = [
        'ukb id': ukb,
        'ukb label': '?',
        'ecmo iri': iri,
        'ecmo label': label
      ]
    } else if(map[ukb]['ecmo iri'] != iri) {
      println 'updating map iri'
      map[ukb]['ecmo iri'] = iri
    }
    if(map[ukb] && map[ukb]['ecmo label'] != label) {
      println 'updating label from ' + map[ukb]['ecmo label'] + ' to ' + label
      map[ukb]['ecmo label'] = label
    }
  }
}

def out = [[ 'ukb id', 'ukb label', 'ecmo iri', 'ecmo label' ].join('\t')] + map.collect { id, val -> [ val['ukb id'], val['ukb label'], val['ecmo iri'], val['ecmo label'] ].join('\t') }
new File('new_map.tsv').text = out.join('\n')
